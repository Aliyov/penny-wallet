package com.penny.wallet.feature

import com.penny.wallet.data.prefs.QuickType
import com.penny.wallet.domain.SplitPart
import com.penny.wallet.feature.add.QuickForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickFormSplitTest {
    private val form = QuickForm(digits = "5000", split = listOf(SplitPart("a", 0), SplitPart("b", 12_50)))

    @Test fun expenseUsesTheBalancedParts() = assertEquals(listOf(37_50L, 12_50L), form.splitParts!!.map { it.amountCents })

    @Test fun otherTypesNeverSaveAsSplit() {
        for (t in listOf(QuickType.INCOME, QuickType.TRANSFER, QuickType.SAVE)) assertNull(form.copy(type = t).splitParts)
    }
}
