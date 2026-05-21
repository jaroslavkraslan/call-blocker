package sk.jaja.callblocker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat

class CustomCallBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // 1. Vytiahneme surové URI hovoru
        val handle: Uri? = callDetails.handle
        val schemeSpecificPart = handle?.schemeSpecificPart?.trim() ?: ""

        // 2. Očistíme číslo od nepovolených znakov
        val phoneNumber = schemeSpecificPart.replace(Regex("[\\s\\-\\(\\)]"), "")

        // LOGIKA BLOKOVANIA
        var shouldBlock = false

        // A) KONTROLA PRE SKRYTÉ / UTAJENÉ ČÍSLA
        if (phoneNumber.isEmpty() ||
            phoneNumber.equals("restricted", ignoreCase = true) ||
            phoneNumber.equals("unknown", ignoreCase = true) ||
            phoneNumber.equals("private", ignoreCase = true)) {

            shouldBlock = true // Skryté čísla blokujeme vždy
        }
        // B) KONTROLA PRE ZOBRAZENÉ ČÍSLA
        else {
            // Definujeme predvoľby krajín, ktoré chceme kompletne prepúšťať (Whitelist)
            val isGerman = phoneNumber.startsWith("+49")
            val isAustrian = phoneNumber.startsWith("+43")
            val isItalian = phoneNumber.startsWith("+39")

            // Ak hovor prichádza z DE, AT alebo IT, neblokujeme ho
            if (isGerman || isAustrian || isItalian) {
                shouldBlock = false
            }
            // Ak ide o slovenské číslo (alebo lokálny formát)
            else {
                val isSlovak = phoneNumber.startsWith("+421") || phoneNumber.startsWith("09") || phoneNumber.startsWith("02")

                if (isSlovak) {
                    // Overíme voči lokálnym kontaktom cez inteligentný PhoneLookup
                    val isSavedInContacts = checkContacts(phoneNumber)
                    if (!isSavedInContacts) {
                        shouldBlock = true // Slovenské číslo mimo kontaktov zablokujeme
                    }
                } else {
                    // Ostatné zahraničné krajiny (mimo DE, AT, IT) necháme prejsť
                    shouldBlock = false
                }
            }
        }

        // 3. Odoslanie rozhodnutia systému
        val response = CallResponse.Builder()
        if (shouldBlock) {
            response.setDisallowCall(true)
            response.setRejectCall(true)
            response.setSkipCallLog(false)      // Uvidíte ich v histórii ako zablokované
            response.setSkipNotification(true)  // Preskočíme predvolenú systémovú, odpálime vlastnú podrobnejšiu

            // Vystrelenie vlastnej notifikácie s informáciou o zablokovanom čísle
            sendBlockNotification(phoneNumber)
        } else {
            response.setDisallowCall(false)
            response.setRejectCall(false)
            response.setSkipCallLog(false)
            response.setSkipNotification(false)
        }

        respondToCall(callDetails, response.build())
    }

    /**
     * Pomocná funkcia na vytvorenie tichej notifikácie s číslom v lište.
     */
    private fun sendBlockNotification(phoneNumber: String) {
        val channelId = "jaja_blocker_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Vytvorenie notifikačného kanála (vyžadované pre Android 8.0+)
        val channel = NotificationChannel(
            channelId,
            "Zablokované hovory",
            NotificationManager.IMPORTANCE_LOW // IMPORTANCE_LOW zabezpečí tiché doručenie bez pípnutia a vibrácií
        ).apply {
            description = "Notifikácie o hovoroch zablokovaných aplikáciou Jaja Blocker"
        }
        notificationManager.createNotificationChannel(channel)

        // Krajšie formátovanie textu, ak je číslo prázdne/skryté
        val displayText = if (phoneNumber.isBlank()) "Skryté / Utajené číslo" else phoneNumber

        // Zostavenie tela notifikácie
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_notification_clear_all) // Použije natívnu systémovú ikonu
            .setContentTitle("Hovor zablokovaný")
            .setContentText("Číslo: $displayText")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true) // Kliknutím sa notifikácia zmaže z lišty
            .build()

        // Unikátne ID podľa času zaistí, že sa notifikácie nebudú prepisovať, ale pekne vrstviť pod seba
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Inteligentné overenie čísla v kontaktoch zariadenia.
     * Využíva PhoneLookup, ktorý ignoruje formátovanie, medzery a úspešne spáruje +421 aj 09 formáty.
     */
    private fun checkContacts(phoneNumber: String): Boolean {
        if (phoneNumber.isEmpty()) return false

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup._ID)
        var isSaved = false

        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    isSaved = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return isSaved
    }
}