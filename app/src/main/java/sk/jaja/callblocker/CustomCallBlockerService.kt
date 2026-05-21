package sk.jaja.callblocker

import android.telecom.Call
import android.telecom.CallScreeningService
import android.net.Uri
import android.provider.ContactsContract

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
        val response = CallScreeningService.CallResponse.Builder()
        if (shouldBlock) {
            response.setDisallowCall(true)
            response.setRejectCall(true)
            response.setSkipCallLog(false)      // Uvidíte ich v histórii ako zablokované
            response.setSkipNotification(false) // Tichá notifikácia v lište (telefón nezazvoní)
        } else {
            response.setDisallowCall(false)
            response.setRejectCall(false)
            response.setSkipCallLog(false)
            response.setSkipNotification(false)
        }

        respondToCall(callDetails, response.build())
    }

    /**
     * Inteligentné overenie čísla v kontaktoch zariadenia.
     * Využíva PhoneLookup, ktorý ignoruje formátovanie, medzery a úspešne spáruje +421 aj 09 formáty.
     */
    private fun checkContacts(phoneNumber: String): Boolean {
        if (phoneNumber.isEmpty()) return false

        // Vytvorenie filtračného URI pre vyhľadávanie konkrétneho čísla
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        // Na overenie existencie nám stačí vytiahnuť ID kontaktu
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)
        var isSaved = false

        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // Ak cursor obsahuje aspoň jeden riadok, číslo sa nachádza v kontaktoch
                    isSaved = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return isSaved
    }
}