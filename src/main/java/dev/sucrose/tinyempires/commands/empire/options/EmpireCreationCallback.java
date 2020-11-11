package dev.sucrose.tinyempires.commands.empire.options;

import org.bson.types.ObjectId;

public interface EmpireCreationCallback {

    void run(ObjectId empireId);

}
