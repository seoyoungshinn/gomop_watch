package com.example.gomop_watch

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.example.gomop_watch.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    //FireBase관련
    private var auth : FirebaseAuth? = null     //FireBase Auth
    var firestore : FirebaseFirestore? = null
    // private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()     //FireBase RealTime
    // private val databaseReference: DatabaseReference = firebaseDatabase.reference       //FireBase RealTime
    private var uid : String? = null



    //여기서부터 onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        Log.d("로그 firebase","파이어베이스 환경세팅")
        auth = Firebase.auth
        Log.d("로그 FireBase","${Firebase}")
        Log.d("로그 auth","$auth")
        firestore = FirebaseFirestore.getInstance()
        Log.d("로그 firestore","$firestore")
        firebaseLogin()                         //파이어베이스 로그인
        uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("로그 firebase","$uid")


    }

    /////////////////////////////////////////////////////////////////////////////
    //                            FIREBASE(DB) 관련 함수                         //
    /////////////////////////////////////////////////////////////////////////////
    fun firebaseLogin(){
        Log.d("로그 파이어베이스 로그인 함수 진입","$auth")
        //고정로그인(테스트용)
        auth?.signInWithEmailAndPassword("test1@test.com", "test123")?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("로그 파이어베이스로그인", "로그인 성공" + "${auth}")
                Log.d("로그 시간", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            } else {
                Log.d("로그 파이어베이스로그인", "로그인 실패" + "${auth}")
            }
        }
    }//End of firebaseLogin()
}