package com.nhl.link.rest.it.fixture.cayenne.auto;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;

import com.nhl.link.rest.it.fixture.cayenne.E8;

/**
 * Class _E9 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _E9 extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String E8_ID_PK_COLUMN = "e8_id";

    public static final Property<E8> E8 = new Property<E8>("e8");

    public void setE8(E8 e8) {
        setToOneTarget("e8", e8, true);
    }

    public E8 getE8() {
        return (E8)readProperty("e8");
    }


}
