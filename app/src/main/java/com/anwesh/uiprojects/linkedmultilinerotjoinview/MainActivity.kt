package com.anwesh.uiprojects.linkedmultilinerotjoinview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.multilinerotview.MultiLineRotView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MultiLineRotView.create(this)
    }
}
