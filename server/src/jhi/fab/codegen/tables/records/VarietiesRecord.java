/*
 * This file is generated by jOOQ.
 */
package jhi.fab.codegen.tables.records;


import jhi.fab.codegen.tables.Varieties;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class VarietiesRecord extends UpdatableRecordImpl<VarietiesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>fab.varieties.variety_id</code>.
     */
    public void setVarietyId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>fab.varieties.variety_id</code>.
     */
    public Integer getVarietyId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>fab.varieties.variety_name</code>.
     */
    public void setVarietyName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>fab.varieties.variety_name</code>.
     */
    public String getVarietyName() {
        return (String) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached VarietiesRecord
     */
    public VarietiesRecord() {
        super(Varieties.VARIETIES);
    }

    /**
     * Create a detached, initialised VarietiesRecord
     */
    public VarietiesRecord(Integer varietyId, String varietyName) {
        super(Varieties.VARIETIES);

        setVarietyId(varietyId);
        setVarietyName(varietyName);
        resetTouchedOnNotNull();
    }

    /**
     * Create a detached, initialised VarietiesRecord
     */
    public VarietiesRecord(jhi.fab.codegen.tables.pojos.Varieties value) {
        super(Varieties.VARIETIES);

        if (value != null) {
            setVarietyId(value.getVarietyId());
            setVarietyName(value.getVarietyName());
            resetTouchedOnNotNull();
        }
    }
}
