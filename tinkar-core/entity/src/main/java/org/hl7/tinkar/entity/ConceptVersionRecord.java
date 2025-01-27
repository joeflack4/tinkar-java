package org.hl7.tinkar.entity;

import io.soabase.recordbuilder.core.RecordBuilder;
import org.hl7.tinkar.component.ConceptVersion;

@RecordBuilder
public record ConceptVersionRecord(ConceptRecord chronology, int stampNid)
        implements ConceptEntityVersion, ConceptVersionRecordBuilder.With {

    public ConceptVersionRecord(ConceptRecord chronology, ConceptVersion version) {
        this(chronology, Entity.nid(version.stamp()));
    }

    @Override
    public ConceptRecord entity() {
        return chronology();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConceptVersionRecord that = (ConceptVersionRecord) o;
        return stampNid == that.stampNid;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(stampNid);
    }

    @Override
    public String toString() {
        return stamp().describe();
    }

}
