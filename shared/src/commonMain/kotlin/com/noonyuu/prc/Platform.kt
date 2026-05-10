package com.noonyuu.prc

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform