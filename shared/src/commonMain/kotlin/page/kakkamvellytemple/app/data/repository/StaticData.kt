package page.kakkamvellytemple.app.data.repository

import page.kakkamvellytemple.app.data.model.*

object StaticData {

    val FESTIVALS = listOf(
        Festival("വിഷു 2026",              "Vishu · Kerala New Year",      "🌸", 1744590600000L, "ദർശനം: 5:00 AM – 7:30 PM", true),
        Festival("അഷ്ടമി രോഹിണി 2026",   "Janmashtami · Birth of Krishna","🪷", 1754326200000L, "ഉണ്ണി കൃഷ്ണൻ ജന്മദിനം"),
        Festival("തിരുവോണം 2026",          "Onam · Thiruvonam",             "🌺", 1757779800000L, "ഓണ ദർശനം"),
        Festival("ഗുരുവായൂർ ഏകാദശി 2026", "Guruvayur Ekadasi",             "🛕", 1764396600000L, "ഏകാദശി വ്രതം"),
        Festival("തിരുവാതിര 2026",          "Thiruvathira",                  "💫", 1766803800000L, "ആർദ്ര ദർശനം"),
        Festival("ശിവരാത്രി 2027",           "Maha Shivaratri",              "🌙", 1771980600000L, "ഊർജ്ജ ആരാധന"),
        Festival("വിഷു 2027",              "Vishu · Kerala New Year",      "🌸", 1776126600000L, "ദർശനം: 5:00 AM – 7:30 PM", true),
        Festival("അഷ്ടമി രോഹിണി 2027",   "Janmashtami · Birth of Krishna","🪷", 1785862200000L, "ഉണ്ണി കൃഷ്ണൻ ജന്മദിനം"),
        Festival("തിരുവോണം 2027",          "Onam · Thiruvonam",             "🌺", 1788229800000L, "ഓണ ദർശനം"),
        Festival("ഗുരുവായൂർ ഏകാദശി 2027", "Guruvayur Ekadasi",             "🛕", 1794846600000L, "ഏകാദശി വ്രതം"),
        Festival("തിരുവാതിര 2027",          "Thiruvathira",                  "💫", 1797340200000L, "ആർദ്ര ദർശനം"),
        Festival("ശിവരാത്രി 2028",           "Maha Shivaratri",              "🌙", 1804602600000L, "ഊർജ്ജ ആരാധന"),
        Festival("വിഷു 2028",              "Vishu · Kerala New Year",      "🌸", 1807576200000L, "ദർശനം: 5:00 AM – 7:30 PM", true),
        Festival("അഷ്ടമി രോഹിണി 2028",   "Janmashtami · Birth of Krishna","🪷", 1817311800000L, "ഉണ്ണി കൃഷ്ണൻ ജന്മദിനം"),
        Festival("തിരുവോണം 2028",          "Onam · Thiruvonam",             "🌺", 1819165800000L, "ഓണ ദർശനം"),
        Festival("ഗുരുവായൂർ ഏകാദശി 2028", "Guruvayur Ekadasi",             "🛕", 1827382200000L, "ഏകാദശി വ്രതം"),
        Festival("തിരുവാതിര 2028",          "Thiruvathira",                  "💫", 1829703000000L, "ആർദ്ര ദർശനം"),
        Festival("ശിവരാത്രി 2029",           "Maha Shivaratri",              "🌙", 1835139000000L, "ഊർജ്ജ ആരാധന"),
        Festival("വിഷു 2029",              "Vishu · Kerala New Year",      "🌸", 1839112200000L, "ദർശനം: 5:00 AM – 7:30 PM", true),
        Festival("അഷ്ടമി രോഹിണി 2029",   "Janmashtami · Birth of Krishna","🪷", 1851225000000L, "ഉണ്ണി കൃഷ്ണൻ ജന്മദിനം"),
        Festival("തിരുവോണം 2029",          "Onam · Thiruvonam",             "🌺", 1852906200000L, "ഓണ ദർശനം"),
        Festival("ഗുരുവായൂർ ഏകാദശി 2029", "Guruvayur Ekadasi",             "🛕", 1859696400000L, "ഏകാദശി വ്രതം"),
        Festival("തിരുവാതിര 2029",          "Thiruvathira",                  "💫", 1861494600000L, "ആർദ്ര ദർശനം"),
    ).sortedBy { it.dateUtcMs }

    val CONTACTS = listOf(
        ContactPerson("തിരുമേനി",                  "Thirumeni",                         "+919941292222"),
        ContactPerson("Secretary — Nanu. T",         "Secretary — Nanu. T",               "+919446844145", "+919446844145"),
        ContactPerson("Jt. Secretary — Chandran P.", "Jt. Secretary — Chandran Pilachery","+918086549927"),
        ContactPerson("Treasurer — Nijeesh KTK",     "Treasurer — Nijeesh KTK",           "+918594039468")
    )

    val MORNING_POOJAS = listOf(
        PoojaItem("നിർമ്മാല്യദർശനം", "Nirmalyam Darshan",   "5:30 AM", 5,  30),
        PoojaItem("ഉഷ പൂജ",           "Usha Pooja",           "6:00 AM", 6,  0),
        PoojaItem("ദീപാരാധന",          "Deepa Aradhana",       "8:00 AM", 8,  0),
        PoojaItem("നട അടക്കൽ",         "Temple Closes",        "9:00 AM", 9,  0)
    )

    val EVENING_POOJAS = listOf(
        PoojaItem("ദീപാരാധന (സന്ധ്യ)", "Evening Deepa Aradhana","5:45 PM", 17, 45),
        PoojaItem("നട അടക്കൽ",          "Temple Closes",          "6:45 PM", 18, 45)
    )

    val GALLERY_PHOTOS = listOf(
        GalleryPhoto("temple_entrance_deepam",  "ക്ഷേത്ര പ്രവേശനം — ദീപം",      "Temple Entrance — Deepam"),
        GalleryPhoto("deepam_lamp_interior",    "ദീപം — ഉൾഭാഗം",                 "Deepam Lamp — Interior"),
        GalleryPhoto("deepam_closeup",          "ദീപ ജ്യോതി — ക്ലോസ്‌അപ്പ്",     "Sacred Flame — Close-up"),
        GalleryPhoto("hanging_lamps_corridor",  "തൂക്കുദീപം — നടപ്പാത",           "Hanging Lamps — Corridor"),
        GalleryPhoto("temple_gate_entrance",    "ക്ഷേത്ര ഗോപുരം",                 "Temple Gate"),
        GalleryPhoto("temple_front_view",       "ക്ഷേത്ര മുൻഭാഗം",               "Temple Front View"),
        GalleryPhoto("temple_side_grounds",     "ക്ഷേത്ര മൈതാനം",                "Temple Grounds")
    )

    // Darshan hours
    const val MORNING_OPEN  = 5 * 60 + 30   // 5:30 AM in minutes
    const val MORNING_CLOSE = 9 * 60          // 9:00 AM
    const val EVENING_OPEN  = 17 * 60 + 45   // 5:45 PM
    const val EVENING_CLOSE = 18 * 60 + 45   // 6:45 PM

    const val TEMPLE_LAT = 11.6814
    const val TEMPLE_LON = 75.6478
    const val WEATHER_URL = "https://api.open-meteo.com/v1/forecast" +
        "?latitude=11.6814&longitude=75.6478" +
        "&current=temperature_2m,weather_code,wind_speed_10m,relative_humidity_2m" +
        "&timezone=Asia%2FKolkata"

    const val YOUTUBE_VIDEO_ID = "SLW7USzalbY"
    const val MAPS_URL = "https://maps.app.goo.gl/LaTA1pWM8NhjW4gK6"
}
