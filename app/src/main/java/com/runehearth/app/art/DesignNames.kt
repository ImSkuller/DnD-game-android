package com.runehearth.app.art

/** Human readable design names, used by the searchable asset picker. */
object DesignNames {
    private val weapons = listOf("Short Sword", "Longsword", "Broadsword", "Rapier", "Scimitar", "Dagger", "Katana", "Battle Axe", "Double Axe", "Hatchet",
        "Spiked Mace", "Flanged Mace", "War Hammer", "Spear", "Halberd", "Trident", "Bow", "Crossbow", "Mage Staff", "Crystal Wand")
    private val shapes = listOf("Round", "Heater", "Kite", "Tower", "Hex")
    private val emblems = listOf("Cross", "Chevron", "Star", "Boss")
    private val helmShapes = listOf("Cap", "Pointed", "Great Helm", "Barbute", "Hood")
    private val helmDecor = listOf("Plumed", "Horned", "Winged", "Spiked")
    private val accessories = listOf("Gold Band", "Gem Ring", "Round Amulet", "Teardrop Pendant", "Cross Pendant", "Bead Necklace", "Belt", "Bracelet", "Earrings", "Crown",
        "Circlet", "Brooch", "Cloak Pin", "Skull Charm", "Ankh", "Medal", "Locket", "Sun Dial", "Hourglass Charm", "Eye Amulet")
    private val potionShapes = listOf("Round Flask", "Tall Vial", "Conical Flask", "Square Bottle", "Long-neck Bulb")
    private val stoppers = listOf("Cork", "Wax Seal", "Skull Stopper", "Crystal")
    private val runeStones = listOf("Tablet", "Pebble", "Hexstone", "Diamond", "Disc")
    private val glyphs = listOf("Fehu", "Thurs", "Algiz", "Othala", "Ansuz", "Dagaz")
    private val herbs = listOf("Sprig", "Clover", "Redcap", "Tallcap", "Daisy", "Tulip", "Rose", "Fern", "Berry Bush", "Wheat", "Thistle", "Lotus", "Moss", "Cactus", "Vine",
        "Bundle", "Mandrake", "Bamboo", "Pine", "Glowcaps")
    private val books = listOf("Clasped Tome", "Guarded Tome", "Gem Tome", "Holy Tome", "Eye Grimoire", "Star Codex", "Strapped Tome", "Open Book", "Scroll", "Book Stack")
    private val chestShapes = listOf("Arched Chest", "Flat Chest", "Barrel", "Strongbox", "Footed Chest")
    private val chestDecor = listOf("Locked", "Open", "Padlocked", "Gem-lidded")
    private val materials = listOf("Ingot", "Ingot Stack", "Ore", "Cut Gem", "Crystals", "Pearl Gem", "Hide", "Bone", "Feather", "Cloth Roll", "Log", "Planks", "Coal", "Scale", "Fang",
        "Claw", "Pearl", "Dust Pouch", "Thread Spool", "Egg")
    private val misc = listOf("Key", "Scroll Case", "Torch", "Lantern", "Coin Pouch", "Map", "Candle", "Hourglass", "Rope", "Skull", "Horn", "Lute", "Bell", "Mug", "Apple", "Bread",
        "Drumstick", "Fish", "Dice", "Crystal Ball")
    private val monsters = listOf("Slime", "Goblin", "Orc", "Skeleton", "Ghost", "Bat", "Spider", "Wolf", "Dragon", "Imp", "Ogre", "Wraith", "Golem", "Serpent", "Mimic", "Treant",
        "Cyclops", "Vampire", "Troll", "Kraken")
    private val gloves = listOf("Gauntlet", "Glove", "Mitten", "Claw", "Bracer")
    private val gloveDecor = listOf("Banded", "Studded", "Knuckled", "Gemmed")
    private val chestA = listOf("Breastplate", "Tunic", "Chainmail", "Robe", "Pauldron Plate")
    private val chestDecor2 = listOf("Plain", "Emblem", "Studded", "Belted")
    private val legs = listOf("Greaves", "Trousers", "Cuisses", "Kilt", "Shorts")
    private val legDecor = listOf("Plain", "Gemmed", "Banded", "Studded")
    private val boots = listOf("Tall Boots", "Sandals", "Sabatons", "Ankle Boots", "Thigh Boots")
    private val bootDecor = listOf("Buckled", "Spurred", "Fur-trimmed", "Winged")

    private fun combo(a: List<String>, b: List<String>, design: Int, bFirst: Boolean = false): String =
        if (bFirst) "${b[design / a.size]} ${a[design % a.size]}" else "${a[design % a.size]} (${b[design / a.size]})"

    fun of(cat: ArtCategory): List<String> = (0 until cat.designs).map { name(cat, it) }

    fun name(cat: ArtCategory, d: Int): String = when (cat) {
        ArtCategory.WEAPON -> weapons[d]
        ArtCategory.OFFHAND -> "${emblems[d / 5]} ${shapes[d % 5]} Shield"
        ArtCategory.HELMET -> "${helmDecor[d / 5]} ${helmShapes[d % 5]}"
        ArtCategory.CHEST_ARMOR -> "${chestDecor2[d / 5]} ${chestA[d % 5]}"
        ArtCategory.GLOVES -> "${gloveDecor[d / 5]} ${gloves[d % 5]}"
        ArtCategory.LEGGINGS -> "${legDecor[d / 5]} ${legs[d % 5]}"
        ArtCategory.BOOTS -> "${bootDecor[d / 5]} ${boots[d % 5]}"
        ArtCategory.ACCESSORY -> accessories[d]
        ArtCategory.POTION -> "${potionShapes[d % 5]} (${stoppers[d / 5]})"
        ArtCategory.RUNE -> "${glyphs[d / 5]} ${runeStones[d % 5]}"
        ArtCategory.HERB -> herbs[d]
        ArtCategory.BOOK -> books[d]
        ArtCategory.CHEST -> "${chestDecor[d / 5]} ${chestShapes[d % 5]}"
        ArtCategory.MATERIAL -> materials[d]
        ArtCategory.MISC -> misc[d]
        ArtCategory.MONSTER -> monsters[d]
    }
}
