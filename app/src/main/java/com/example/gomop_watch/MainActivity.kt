package com.example.gomop_watch

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gomop_watch.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import model.*

import utils.Constant.API.LOG
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var x: TextView
    lateinit var y: TextView
    lateinit var btn : Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest:LocationRequest
    private val REQUEST_PERMISSION_LOCATION = 10
    val ACTIVITY_RECOGNITION_REQUEST_CODE = 100

    //FireBase관련
    private var auth : FirebaseAuth? = null     //FireBase Auth
    var firestore : FirebaseFirestore? = null
    // private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()     //FireBase RealTime
    // private val databaseReference: DatabaseReference = firebaseDatabase.reference       //FireBase RealTime
    private var uid : String? = null
    private var id :String? = null



    //음성출력관련
    private lateinit var tts: TextToSpeech

    // 현재위치
    private var lat: Double = 0.0
    private var lon: Double = 0.0
//    private var lat: Double = 37.58217852030164
//    private var lon: Double = 127.01152516595631



    //현재위치조정완료
    private var modified = 0

    //진동관련
    lateinit var vibrator: Vibrator

    //클릭 이벤트 시간 저장할 변수
    private var clickTime: Long = 0

    //클릭 중복 방지 변수
    private var clickNum = 0

    //도착시 즐겨찾기 등록 활성화
    private var dofavor = false



    //여기서부터 onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTTS()        //TTS세팅 및 초기화



        //FireBase환경세팅
        Log.d("firebase","파이어베이스 환경세팅")
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()
        firebaseLogin()                         //파이어베이스 로그인
        uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("firebase","$uid")







        //화면이 꺼지지 않게
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)


        //레이아웃 세팅
        x = findViewById<TextView>(R.id.x)
        y = findViewById<TextView>(R.id.y)
        //btn = findViewById<Button>(R.id.btn)
        x.setText(lon.toString())
        y.setText(lat.toString())




        //진동관련
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        //통합 위치 정보 제공자 클라이언트의 인스턴스
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startLocationUpdates()



    }

    //위치를 1초에 한번씩 갱신한다
    private fun startLocationUpdates() {
        if (checkPermissionForLocation(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

            }
            fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
        }
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (locationResult == null) {
                return
            }

            for (location in locationResult.locations) {
                if (location != null) {
                    lon = location.longitude
                    lat = location.latitude

                    x.setText(lon.toString())
                    y.setText(lat.toString())

                    modified++
                    if(modified==1){
                        //현재위치가 조정 완료되었다는 tts
                        ttsSpeak("실시간 위치 파악중입니다. 버튼을 누르면 내 위치를 SNS에 업데이트합니다")
                        val effect = VibrationEffect.createOneShot(500, 100)
                        vibrator.vibrate(effect)
                    }

                    Log.d(LOG,"MainActivity - 현재위치 : ["+"${lat}"+", "+"${lon}"+"]")

                }
            }
        }
    }




    //물리버튼을 눌러 STT를 실행
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            // 두번 클릭시 즐겨찾기 등록
            if (SystemClock.elapsedRealtime() - clickTime < 500 ) {
                clickNum = 2
                startSTT(10)
                overridePendingTransition(0, 0)
            }
            else {
                clickNum = 1
            }
            Handler().postDelayed(java.lang.Runnable {
                if (clickNum == 1){
                    startSTT(0)
                }
            },500)

            clickTime = SystemClock.elapsedRealtime()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startSTT(intentNum:Int){
        //SpeechToTextActivity 실행
        if (intentNum == 0) { //목적지입력시
            if (modified < 2) {
                ttsSpeak("위치 조정 중")
            } else {
             //   History.dpLat = lat     //DB 저장용
              //  History.dpLon = lon     // DB저장용
                ttsSpeak("현재 위치를 나의 sns에 공유했습니다.")
                getMyLocation(lat,lon);
            }
        }

    }


    //다른 Activity로부터 결과값 받기
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

    }


    /////////////////////////////////////////////////////////////////////////////
    //                            FIREBASE(DB) 관련 함수                         //
    /////////////////////////////////////////////////////////////////////////////
    fun firebaseLogin(){
        //고정로그인(테스트용)
        auth?.signInWithEmailAndPassword("test1@test.com", "test123")?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("파이어베이스로그인", "로그인 성공" + "${uid}")
                Log.d("시간",LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                getDataFromDB()
            } else {
                Log.d("파이어베이스로그인", "로그인 실패" + "${uid}")
            }
        }
    }//End of firebaseLogin()

    //이제 안씀
    private fun getDataFromDB() {
        var snapshotData: Map<String, Any>
        var followings: Map<String, String>
        var followers: Map<String, String>
        var dtr = firestore?.collection("uid")?.document(uid.toString())
        dtr?.get()?.addOnSuccessListener { doc ->
            if (doc != null) {
                snapshotData = doc.data as Map<String, Any>
             //   followers = snapshotData.get("followers") as Map<String, String>
             //   followings = snapshotData.get("followings") as Map<String, String>
                Log.d("로그 snapshotData: ", snapshotData.toString())
             //   Log.d("로그 followers: ", followers.toString())
             //   Log.d("로그 followings: ", followings.toString())
           //     MyLocation.followers = followers
             //   MyLocation.followings = followings
                id  = snapshotData.get("id") as String
                Log.d("로그 :::", id!!)
                MyLocation.id = id?:"loading"
                MyLocation.id = snapshotData.get("id") as String
                MyLocation.lat = snapshotData.get("lat") as Double
                MyLocation.lon = snapshotData.get("lon") as Double
                MyLocation.updateTime = snapshotData.get("updateTime") as String
            }
        }
    }

    private fun getMyLocation(lat: Double, lon: Double) {
        //현재 위치를 싱글톤 객체에 저장
        MyLocation.lat =lat
        MyLocation.lon = lon
        Log.d("로그","위치받아왔음")
        MyLocation.updateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        MyLocation.id = id!!
        sendMyLocation(uid!!);
    }

    private fun sendMyLocation(uid:String) {
        //싱글톤 객체에 저장된 나의 위치를 DB에 업데이트
        firestore?.collection("uid")?.document(uid)?.set(MyLocation)
        Log.d("로그","DB에위치업데이트완료")
    }



    /*----------------------------!안 건들여도 됨!------------------------------------------
    -------------------------------------------------------------------------------------
    -------------------------------------------------------------------------------------
    -------------------------------------------------------------------------------------
    -------------------------------------------------------------------------------------
    -------------------------------------------------------------------------------------*/
    //사용자 권한, 바꿀거 없음.
    override fun onStart() {
        super.onStart()
        //권한승인여부 확인후 메시지 띄워줌(둘 중 하나라도)
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                0
            )
        }

        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    100
                )
            }
        }

        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BODY_SENSORS),
                    100
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACTIVITY_RECOGNITION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                }
            }
        }
    }


    // 위치 권한
    private fun checkPermissionForLocation(context: Context): Boolean {
        // Android 6.0 Marshmallow 이상에서는 위치 권한에 추가 런타임 권한이 필요
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                // 권한이 없으므로 권한 요청 알림 보내기
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_LOCATION)
                false
            }
        } else {
            true
        }
    }
    //음성출력
    private fun ttsSpeak(strTTS:String){
        tts.speak(strTTS, TextToSpeech.QUEUE_ADD,null,null)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun setTTS(){
        // TTS를 생성하고 OnInitListener로 초기화 한다.
        tts= TextToSpeech(this){
            if(it==TextToSpeech.SUCCESS){
                val result = tts?.setLanguage(Locale.KOREAN)
                if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                    Log.d("로그","지원하지 않은 언어")
                    return@TextToSpeech
                }
                Log.d("로그","TTS 세팅 성공")
            }else{
                Log.d("로그","TTS 세팅 실패")
            }
        }
    }
}