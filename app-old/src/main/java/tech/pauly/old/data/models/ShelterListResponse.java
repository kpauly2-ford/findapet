package tech.pauly.old.data.models;


import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

import androidx.annotation.Nullable;

@Root(name = "petfinder", strict = false)
public class ShelterListResponse {

    @Element(name = "header")
    private Header header;

    @Element(required = false)
    private int lastOffset;

    @ElementList(name = "shelters", entry = "shelter", required = false)
    private List<Shelter> shelterList;

    @Nullable
    public List<Shelter> getShelterList() {
        return shelterList;
    }
}