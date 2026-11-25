package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object PortalIconMapper {
    fun resolveIcon(iconKey: String, defaultIcon: ImageVector = Icons.Default.Category): ImageVector {
        return when (iconKey.trim().lowercase()) {
            "badge" -> Icons.Default.Badge
            "child_care", "childcare" -> Icons.Default.ChildCare
            "landscape" -> Icons.Default.Landscape
            "flight_takeoff", "flighttakeoff" -> Icons.Default.FlightTakeoff
            "verified_user", "verifieduser" -> Icons.Default.VerifiedUser
            "security" -> Icons.Default.Security
            "storefront" -> Icons.Default.Storefront
            "directions_car", "directionscar" -> Icons.Default.DirectionsCar
            "receipt_long", "receiptlong" -> Icons.Default.ReceiptLong
            "hub" -> Icons.Default.Hub
            "emergency" -> Icons.Default.Emergency
            "work" -> Icons.Default.Work
            "school" -> Icons.Default.School
            "poll" -> Icons.Default.Poll
            "volunteer_activism", "volunteeractivism" -> Icons.Default.VolunteerActivism
            "bar_chart", "barchart" -> Icons.Default.BarChart
            "smart_toy", "smarttoy" -> Icons.Default.SmartToy
            "travel_explore", "travelexplore" -> Icons.Default.TravelExplore
            "bolt" -> Icons.Default.Bolt
            "add_road", "addroad" -> Icons.Default.AddRoad
            "shield" -> Icons.Default.Shield
            "local_hospital", "localhospital" -> Icons.Default.LocalHospital
            "foundation" -> Icons.Default.Foundation
            "directions_boat", "directionsboat" -> Icons.Default.DirectionsBoat
            "beach_access", "beachaccess" -> Icons.Default.BeachAccess
            "water_drop", "waterdrop" -> Icons.Default.WaterDrop
            "agriculture" -> Icons.Default.Agriculture
            "blood", "bloodtype" -> Icons.Default.Bloodtype
            "favorite" -> Icons.Default.Favorite
            "info" -> Icons.Default.Info
            "groups" -> Icons.Default.Groups
            "account_balance", "accountbalance" -> Icons.Default.AccountBalance
            else -> defaultIcon
        }
    }
}
