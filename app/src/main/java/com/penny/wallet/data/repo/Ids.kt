package com.penny.wallet.data.repo

import java.util.UUID

internal fun newId(): String = UUID.randomUUID().toString()
internal fun now(): Long = System.currentTimeMillis()

/** Called after every DB write (widgets refresh, etc.). */
fun interface ChangeListener { fun onDataChanged() }
