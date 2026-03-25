package com.gamematcher.dto.riot;

public class RiotStatsResponseDto {

    private String puuid;
    private String gameName;
    private String tagLine;
    private int games;
    private int wins;
    private int losses;
    private double winRate;
    private double avgKills;
    private double avgDeaths;
    private double avgAssists;
    private String mostPlayedChampion;

    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }

    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }

    public String getTagLine() { return tagLine; }
    public void setTagLine(String tagLine) { this.tagLine = tagLine; }

    public int getGames() { return games; }
    public void setGames(int games) { this.games = games; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }

    public double getAvgKills() { return avgKills; }
    public void setAvgKills(double avgKills) { this.avgKills = avgKills; }

    public double getAvgDeaths() { return avgDeaths; }
    public void setAvgDeaths(double avgDeaths) { this.avgDeaths = avgDeaths; }

    public double getAvgAssists() { return avgAssists; }
    public void setAvgAssists(double avgAssists) { this.avgAssists = avgAssists; }

    public String getMostPlayedChampion() { return mostPlayedChampion; }
    public void setMostPlayedChampion(String mostPlayedChampion) { this.mostPlayedChampion = mostPlayedChampion; }
}
