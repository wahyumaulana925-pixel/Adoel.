package com.jekael.adoel.data

fun buildDefaultDb(): Map<String, MesinData> {
    val db = mutableMapOf<String, MesinData>()
    for (i in 1..174) db["$i"] = MesinData(tipe = MesinTipe.TAPPET, corak = "-")

    listOf(29, 30, 31, 32, 33, 34, 35, 39, 42, 43, 45, 47, 49, 51, 53).forEach {
        db["$it"] = MesinData(MesinTipe.TAPPET, "34758", targetYard = 303.0)
    }
    db["36"] = MesinData(MesinTipe.TAPPET, "15976", targetYard = 303.0)
    listOf(37, 38, 40, 41).forEach { db["$it"] = MesinData(MesinTipe.TAPPET, "92976", targetYard = 303.0) }
    db["55"] = MesinData(MesinTipe.TAPPET, "35758", targetYard = 303.0)

    listOf(57, 58, 59, 60, 63, 64, 71, 72).forEach { db["$it"] = MesinData(MesinTipe.D405, "-") }
    listOf(61, 62, 68).forEach { db["$it"] = MesinData(MesinTipe.D405, "60357", targetYard = 303.0, speed = 0.158) }
    listOf(65, 70).forEach { db["$it"] = MesinData(MesinTipe.D405, "60357", targetYard = 303.0, speed = 0.156) }
    listOf(66, 69).forEach { db["$it"] = MesinData(MesinTipe.D405, "60357", targetYard = 303.0, speed = 0.1625) }
    db["67"] = MesinData(MesinTipe.D405, "60357", targetYard = 303.0, speed = 0.145)
    db["85"] = MesinData(MesinTipe.D405, "60357", targetYard = 303.0, speed = 0.154)
    db["86"] = MesinData(MesinTipe.D405, "60357", targetYard = 303.0, speed = 0.146)
    db["87"] = MesinData(MesinTipe.D405, "60357", targetYard = 303.0, speed = 0.158)
    db["88"] = MesinData(MesinTipe.D405, "60357", targetYard = 303.0, speed = 0.152)

    listOf(73, 74, 75, 77, 78, 89, 90, 91, 92, 93, 94, 117, 119).forEach {
        db["$it"] = MesinData(MesinTipe.CAM, "-")
    }
    listOf(76, 95, 96, 97, 99, 100, 102).forEach {
        db["$it"] = MesinData(MesinTipe.CAM, "21242", targetYard = 165.0)
    }
    listOf(98, 101, 103, 104).forEach {
        db["$it"] = MesinData(MesinTipe.CAM, "66335", targetYard = 165.0)
    }

    db["79"] = MesinData(MesinTipe.D408, "60357", targetYard = 303.0, koreksi = 18.0)
    db["80"] = MesinData(MesinTipe.D408, "60357", targetYard = 303.0, koreksi = 16.0)
    db["81"] = MesinData(MesinTipe.D408, "60357", targetYard = 303.0, koreksi = 22.0)
    db["82"] = MesinData(MesinTipe.D408, "60357", targetYard = 303.0, koreksi = 17.0)
    db["83"] = MesinData(MesinTipe.D408, "60357", targetYard = 303.0, koreksi = 16.0)
    db["84"] = MesinData(MesinTipe.D408, "60357", targetYard = 303.0, koreksi = 23.0)

    return db
}
