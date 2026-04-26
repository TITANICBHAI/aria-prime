package com.aiassistant.ml;

import androidx.annotation.NonNull;

/**
 * Enumeration of game types supported by the AI Assistant.
 */
public enum GameType {
    /**
     * Action games (platformers, shooters, etc.)
     */
    ACTION("action"),
    
    /**
     * Role-playing games (RPGs)
     */
    RPG("rpg"),
    
    /**
     * Strategy games
     */
    STRATEGY("strategy"),
    
    /**
     * Puzzle games
     */
    PUZZLE("puzzle"),
    
    /**
     * Racing games
     */
    RACING("racing"),
    
    /**
     * Sports games
     */
    SPORTS("sports"),
    
    /**
     * Simulation games
     */
    SIMULATION("simulation"),
    
    /**
     * Adventure games
     */
    ADVENTURE("adventure"),
    
    /**
     * Card games
     */
    CARD("card"),
    
    /**
     * Board games
     */
    BOARD("board"),
    
    /**
     * Educational games
     */
    EDUCATIONAL("educational"),
    
    /**
     * Casual games
     */
    CASUAL("casual"),
    
    /**
     * Idle/clicker games
     */
    IDLE("idle"),
    
    /**
     * Music/rhythm games
     */
    MUSIC("music"),
    
    /**
     * Fighting games
     */
    FIGHTING("fighting"),
    
    /**
     * Multiplayer online battle arena (MOBA) games
     */
    MOBA("moba"),
    
    /**
     * Battle royale games
     */
    BATTLE_ROYALE("battle_royale"),
    
    /**
     * Unknown or unclassified game type
     */
    UNKNOWN("unknown");
    
    private final String value;
    
    GameType(String value) {
        this.value = value;
    }
    
    /**
     * Get the string value of the game type
     * 
     * @return String value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Convert a string to a GameType
     * 
     * @param value String value
     * @return GameType or UNKNOWN if not found
     */
    @NonNull
    public static GameType fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        
        String lowerValue = value.toLowerCase();
        
        for (GameType gameType : values()) {
            if (gameType.value.equals(lowerValue)) {
                return gameType;
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * Determine game type from package name
     * 
     * @param packageName App package name
     * @return GameType or UNKNOWN if not a recognized game
     */
    @NonNull
    public static GameType fromPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return UNKNOWN;
        }
        
        String lowerPackage = packageName.toLowerCase();
        
        // Battle Royale / FPS games
        if (lowerPackage.contains("pubg") || lowerPackage.contains("playerunknown")) {
            return BATTLE_ROYALE;
        } else if (lowerPackage.contains("fortnite")) {
            return BATTLE_ROYALE;
        } else if (lowerPackage.contains("freefire") || lowerPackage.contains("garena")) {
            return BATTLE_ROYALE;
        } else if (lowerPackage.contains("callofduty") || lowerPackage.contains("cod")) {
            return ACTION;
        } else if (lowerPackage.contains("battlefield")) {
            return ACTION;
        }
        
        // MOBA games
        if (lowerPackage.contains("leagueoflegends") || lowerPackage.contains("league")) {
            return MOBA;
        } else if (lowerPackage.contains("dota")) {
            return MOBA;
        } else if (lowerPackage.contains("mobilelegends")) {
            return MOBA;
        } else if (lowerPackage.contains("vainglory")) {
            return MOBA;
        } else if (lowerPackage.contains("pokemon") && lowerPackage.contains("unite")) {
            return MOBA;
        }
        
        // Strategy games
        if (lowerPackage.contains("clash") && lowerPackage.contains("clans")) {
            return STRATEGY;
        } else if (lowerPackage.contains("clash") && lowerPackage.contains("royale")) {
            return STRATEGY;
        } else if (lowerPackage.contains("brawlstars")) {
            return STRATEGY;
        } else if (lowerPackage.contains("ageofempires")) {
            return STRATEGY;
        }
        
        // RPG games
        if (lowerPackage.contains("genshinimpact") || lowerPackage.contains("genshin")) {
            return RPG;
        } else if (lowerPackage.contains("finalfantasy")) {
            return RPG;
        } else if (lowerPackage.contains("pokemon") && !lowerPackage.contains("unite")) {
            return RPG;
        }
        
        // Racing games
        if (lowerPackage.contains("asphalt")) {
            return RACING;
        } else if (lowerPackage.contains("needforspeed")) {
            return RACING;
        } else if (lowerPackage.contains("realracing")) {
            return RACING;
        }
        
        // Puzzle games
        if (lowerPackage.contains("candy") && lowerPackage.contains("crush")) {
            return PUZZLE;
        } else if (lowerPackage.contains("wordswithfriends")) {
            return PUZZLE;
        } else if (lowerPackage.contains("2048")) {
            return PUZZLE;
        }
        
        // Sports games
        if (lowerPackage.contains("fifa")) {
            return SPORTS;
        } else if (lowerPackage.contains("nba")) {
            return SPORTS;
        } else if (lowerPackage.contains("madden")) {
            return SPORTS;
        }
        
        // Generic detection based on keywords
        if (lowerPackage.contains("game") || lowerPackage.contains("play")) {
            // Generic game, but we can't determine the type
            return UNKNOWN;
        }
        
        // Not recognized as a game
        return UNKNOWN;
    }
    
    @Override
    public String toString() {
        return value;
    }
}