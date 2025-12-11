package com.lancontrol.utils

import java.time.LocalTime

/**
 * Manages time-based access restrictions
 */
object TimeRestriction {
    
    private const val ADMIN_PASSWORD = "12345ERD54321"
    private const val UNLOCK_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    
    @Volatile
    private var unlockUntil: Long = 0
    
    // Restricted time periods (lesson hours)
    private val restrictedPeriods = listOf(
        LocalTime.of(8, 10) to LocalTime.of(8, 50),
        LocalTime.of(9, 0) to LocalTime.of(9, 40),
        LocalTime.of(9, 50) to LocalTime.of(10, 30),
        LocalTime.of(10, 40) to LocalTime.of(11, 20),
        LocalTime.of(11, 30) to LocalTime.of(12, 10),
        LocalTime.of(13, 20) to LocalTime.of(14, 0),
        LocalTime.of(14, 10) to LocalTime.of(14, 50),
        LocalTime.of(15, 0) to LocalTime.of(15, 40)
        
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
        
        return restrictedPeriods.any { (start, end) ->
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
}
