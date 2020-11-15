package dev.sucrose.tinyempires.models;

import org.bson.types.ObjectId;

public interface EmpireCreationCallback {

    void run(ObjectId empireId);

}
