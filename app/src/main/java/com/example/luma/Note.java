package com.example.luma;

public class Note {
    private int id;
    private String title;
    private String content;
    private String alarmTime; // Nouveau champ pour stocker l'heure de l'alarme

    // Constructeur avec tous les champs
    public Note(int id, String title, String content, String alarmTime) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.alarmTime = alarmTime;
    }

    // Constructeur sans l'ID (pour les nouvelles notes)
    public Note(String title, String content) {
        this.id = -1; // Valeur par défaut pour une nouvelle note
        this.title = title;
        this.content = content;
        this.alarmTime = null; // Par défaut, aucune alarme définie
    }

    // Constructeur sans alarme (si vous voulez garder la compatibilité avec l'ancien code)
    public Note(int id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.alarmTime = null; // Aucune alarme par défaut
    }

    // Getters et Setters pour ID
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Getters et Setters pour Title
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // Getters et Setters pour Content
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // Getters et Setters pour AlarmTime
    public String getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(String alarmTime) {
        this.alarmTime = alarmTime;
    }
}

