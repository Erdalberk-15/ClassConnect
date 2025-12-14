package com.lancontrol.utils

import java.time.LocalTime
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Manages time-based access restrictions
 */
object TimeRestriction {
    
    private const val ADMIN_PASSWORD = "135791113151719EBP54321"
    private const val UNLOCK_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    
    @Volatile
    private var unlockUntil: Long = 0
    
    // Restricted time periods for Monday-Thursday
    private val weekdayRestrictedPeriods = listOf(
        LocalTime.of(8, 10) to LocalTime.of(8, 50),
        LocalTime.of(9, 0) to LocalTime.of(9, 40),
        LocalTime.of(9, 50) to LocalTime.of(10, 30),
        LocalTime.of(10, 40) to LocalTime.of(11, 20),
        LocalTime.of(11, 30) to LocalTime.of(12, 10),
        LocalTime.of(13, 20) to LocalTime.of(14, 0),
        LocalTime.of(14, 10) to LocalTime.of(14, 50),
        LocalTime.of(15, 0) to LocalTime.of(15, 40)
    )
    
    // Restricted time periods for Friday (different schedule)
    private val fridayRestrictedPeriods = listOf(
        LocalTime.of(8, 10) to LocalTime.of(8, 50),
        LocalTime.of(9, 0) to LocalTime.of(9, 40),
        LocalTime.of(9, 50) to LocalTime.of(10, 30),
        LocalTime.of(10, 40) to LocalTime.of(11, 20),
        LocalTime.of(11, 30) to LocalTime.of(12, 10),
        LocalTime.of(13, 20) to LocalTime.of(14, 0),
        LocalTime.of(14, 5) to LocalTime.of(14, 45),
        LocalTime.of(14, 50) to LocalTime.of(15, 30)
    )
    
    /**
     * Check if current time is within restricted period
     */
    fun isRestricted(): Boolean {
        // Check if temporarily unlocked
        if (System.currentTimeMillis() < unlockUntil) {
            return false
        }
        
        val now = LocalTime.now()
        val today = LocalDate.now().dayOfWeek
        
        // Weekend - no restrictions
        if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) {
            return false
        }
        
        // Select appropriate schedule based on day
        val periods = if (today == DayOfWeek.FRIDAY) {
            fridayRestrictedPeriods
        } else {
            weekdayRestrictedPeriods
        }
        
        return periods.any { (start, end) ->
            now >= start && now <= end
        }
    }
    
    /**
     * Try to unlock with admin password
     * Returns true if password is correct
     */
    fun tryUnlock(password: String): Boolean {
        if (password == ADMIN_PASSWORD) {
            unlockUntil = System.currentTimeMillis() + UNLOCK_DURATION_MS
            return true
        }
        return false
    }
    
    /**
     * Get remaining unlock time in seconds
     */
    fun getRemainingUnlockSeconds(): Int {
        val remaining = unlockUntil - System.currentTimeMillis()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }
    
    /**
     * Check if currently unlocked by admin
     */
    fun isUnlocked(): Boolean {
        return System.currentTimeMillis() < unlockUntil
    }
    
    /**
     * Lock the application (reset admin unlock)
     */
    fun lock() {
        unlockUntil = 0
    }
}
