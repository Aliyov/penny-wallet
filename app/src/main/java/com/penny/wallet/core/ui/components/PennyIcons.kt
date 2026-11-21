package com.penny.wallet.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icon set for accounts, categories and goals.
 * Keys are persisted in the DB and in backups: never rename or remove a key, only add new ones.
 */
object PennyIcons {

    val all: List<Pair<String, ImageVector>> = listOf(
        "bank" to Icons.Rounded.AccountBalance,
        "cash" to Icons.Rounded.Payments,
        "card" to Icons.Rounded.CreditCard,
        "revolut" to Icons.Rounded.Payment,
        "wallet" to Icons.Rounded.AccountBalanceWallet,
        "savings" to Icons.Rounded.Savings,
        "piggy" to Icons.Rounded.Toll,
        "salary" to Icons.Rounded.Work,
        "invest" to Icons.AutoMirrored.Rounded.TrendingUp,
        "crypto" to Icons.Rounded.CurrencyBitcoin,
        "euro" to Icons.Rounded.Euro,
        "paid" to Icons.Rounded.Paid,
        "atm" to Icons.Rounded.Atm,
        "tax" to Icons.AutoMirrored.Rounded.ReceiptLong,
        "insurance" to Icons.Rounded.Shield,
        "diamond" to Icons.Rounded.Diamond,
        "home" to Icons.Rounded.Home,
        "rent" to Icons.Rounded.Apartment,
        "key" to Icons.Rounded.Key,
        "electricity" to Icons.Rounded.Bolt,
        "light" to Icons.Rounded.Lightbulb,
        "water" to Icons.Rounded.WaterDrop,
        "gas" to Icons.Rounded.PropaneTank,
        "wifi" to Icons.Rounded.Wifi,
        "phone" to Icons.Rounded.Smartphone,
        "tablet" to Icons.Rounded.Tablet,
        "laptop" to Icons.Rounded.Laptop,
        "computer" to Icons.Rounded.Computer,
        "tv" to Icons.Rounded.Tv,
        "headphones" to Icons.Rounded.Headphones,
        "watch" to Icons.Rounded.Watch,
        "subscription" to Icons.Rounded.Subscriptions,
        "cloud" to Icons.Rounded.Cloud,
        "ai" to Icons.Rounded.AutoAwesome,
        "tools" to Icons.Rounded.Build,
        "repair" to Icons.Rounded.Handyman,
        "construction" to Icons.Rounded.Construction,
        "cleaning" to Icons.Rounded.CleaningServices,
        "laundry" to Icons.Rounded.LocalLaundryService,
        "garden" to Icons.Rounded.Yard,
        "groceries" to Icons.Rounded.LocalGroceryStore,
        "restaurant" to Icons.Rounded.Restaurant,
        "dinner" to Icons.Rounded.DinnerDining,
        "lunch" to Icons.Rounded.LunchDining,
        "fastfood" to Icons.Rounded.Fastfood,
        "pizza" to Icons.Rounded.LocalPizza,
        "coffee" to Icons.Rounded.LocalCafe,
        "bar" to Icons.Rounded.LocalBar,
        "wine" to Icons.Rounded.WineBar,
        "icecream" to Icons.Rounded.Icecream,
        "cake" to Icons.Rounded.Cake,
        "car" to Icons.Rounded.DirectionsCar,
        "fuel" to Icons.Rounded.LocalGasStation,
        "parking" to Icons.Rounded.LocalParking,
        "taxi" to Icons.Rounded.LocalTaxi,
        "bus" to Icons.Rounded.DirectionsBus,
        "train" to Icons.Rounded.Train,
        "bike" to Icons.AutoMirrored.Rounded.DirectionsBike,
        "commute" to Icons.Rounded.Commute,
        "plane" to Icons.Rounded.Flight,
        "vacation" to Icons.Rounded.Luggage,
        "beach" to Icons.Rounded.BeachAccess,
        "hotel" to Icons.Rounded.Hotel,
        "hiking" to Icons.Rounded.Hiking,
        "shipping" to Icons.Rounded.LocalShipping,
        "shopping" to Icons.Rounded.ShoppingBag,
        "cart" to Icons.Rounded.ShoppingCart,
        "store" to Icons.Rounded.Storefront,
        "clothes" to Icons.Rounded.Checkroom,
        "beauty" to Icons.Rounded.Spa,
        "haircut" to Icons.Rounded.Brush,
        "gift" to Icons.Rounded.CardGiftcard,
        "camera" to Icons.Rounded.PhotoCamera,
        "health" to Icons.Rounded.Favorite,
        "pharmacy" to Icons.Rounded.LocalPharmacy,
        "medication" to Icons.Rounded.Medication,
        "hospital" to Icons.Rounded.LocalHospital,
        "gym" to Icons.Rounded.FitnessCenter,
        "sport" to Icons.Rounded.SportsSoccer,
        "pool" to Icons.Rounded.Pool,
        "pet" to Icons.Rounded.Pets,
        "baby" to Icons.Rounded.ChildCare,
        "school" to Icons.Rounded.School,
        "book" to Icons.AutoMirrored.Rounded.MenuBook,
        "charity" to Icons.Rounded.VolunteerActivism,
        "smoking" to Icons.Rounded.SmokingRooms,
        "music" to Icons.Rounded.MusicNote,
        "movie" to Icons.Rounded.Movie,
        "cinema" to Icons.Rounded.Theaters,
        "theater" to Icons.Rounded.TheaterComedy,
        "game" to Icons.Rounded.SportsEsports,
        "party" to Icons.Rounded.Celebration,
        "park" to Icons.Rounded.Park,
        "star" to Icons.Rounded.Star,
        "other" to Icons.Rounded.Category,
        "more" to Icons.Rounded.MoreHoriz,
        "transfer" to Icons.Rounded.SwapHoriz,
    )

    private val map: Map<String, ImageVector> = all.toMap()

    fun get(key: String): ImageVector = map[key] ?: Icons.Rounded.Category

    /** Subset shown first / only when picking an account icon. */
    val accountKeys: List<String> = listOf(
        "bank", "cash", "card", "revolut", "wallet", "savings", "piggy", "euro",
        "paid", "atm", "invest", "crypto", "salary", "diamond", "home", "other",
    )
}
