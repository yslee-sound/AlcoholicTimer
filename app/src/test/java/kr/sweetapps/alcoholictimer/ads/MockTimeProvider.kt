package kr.sweetapps.alcoholictimer.ads

class MockTimeProvider(var nowMillis: Long = System.currentTimeMillis()) {
    fun currentTimeMillis(): Long = nowMillis
    fun advanceBy(millis: Long) { nowMillis += millis }
}

