package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object CustomerSupportHelper {
    const val SUPPORT_WHATSAPP_NUMBER = "9433656298"
    const val SUPPORT_WHATSAPP_DISPLAY = "+91 9433656298"

    fun openWhatsApp(context: Context, defaultMessage: String = "Hello Study With Buddy Support, I need help with the app.") {
        try {
            val encodedMsg = try {
                URLEncoder.encode(defaultMessage, "UTF-8")
            } catch (e: Exception) {
                "Hello+Study+With+Buddy+Support"
            }
            val url = "https://api.whatsapp.com/send?phone=91$SUPPORT_WHATSAPP_NUMBER&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Customer Support WhatsApp: $SUPPORT_WHATSAPP_DISPLAY", Toast.LENGTH_LONG).show()
        }
    }

    fun callSupport(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$SUPPORT_WHATSAPP_NUMBER")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Support Number: $SUPPORT_WHATSAPP_DISPLAY", Toast.LENGTH_SHORT).show()
        }
    }
}
