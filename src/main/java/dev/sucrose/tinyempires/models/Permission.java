package dev.sucrose.tinyempires.models;

public enum Permission {
    // grants all permissions
    ADMIN,
    // can sell and buy chunks
    CHUNKS,
    // can give a permission to another member if they have it
    POSITIONS,
    // can take funds from reserve
    RESERVE,
    // can write and publish laws
    LAWS,
    // can declare and end wars
    WAR,
    // can change color and edit description
    EDIT,
    // can add and accept players
    INVITES,
    // can change empire home
    HOME
}
