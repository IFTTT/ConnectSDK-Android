package com.ifttt.location;

import com.ifttt.connect.Feature;
import com.ifttt.connect.UserFeatureField;
import java.util.List;

interface GeofenceProvider {

    void updateGeofences(final List<Feature> features);

}
