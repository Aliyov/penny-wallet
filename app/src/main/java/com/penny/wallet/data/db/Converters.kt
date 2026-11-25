package com.penny.wallet.data.db

import androidx.room.TypeConverter
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.data.db.entities.TxType
import java.time.LocalDate

/** LocalDate is stored as epoch day so range queries (months) are plain integer comparisons. */
class Converters {
    @TypeConverter fun dateToLong(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun longToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
    @TypeConverter fun txTypeToString(t: TxType): String = t.name
    @TypeConverter fun stringToTxType(s: String): TxType = TxType.valueOf(s)
    @TypeConverter fun freqToString(f: Frequency): String = f.name
    @TypeConverter fun stringToFreq(s: String): Frequency = Frequency.valueOf(s)
    @TypeConverter fun debtDirectionToString(d: DebtDirection): String = d.name
    @TypeConverter fun stringToDebtDirection(s: String): DebtDirection = DebtDirection.valueOf(s)
}
