package org.blrdroid

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform