package com.mohdharish.geolocapp.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mohdharish.geolocapp.R

class MapFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val radiusSeekBar = view.findViewById<android.widget.SeekBar>(R.id.radiusSeekBar)
        val saveButton = view.findViewById<android.widget.Button>(R.id.saveGeofenceButton)
        var lastPin: com.google.android.gms.maps.model.LatLng? = null
        var lastRadius: Int = radiusSeekBar.progress
        var circle: com.google.android.gms.maps.model.Circle? = null

        if (childFragmentManager.findFragmentById(R.id.map_container) == null) {
            val mapFragment = com.google.android.gms.maps.SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit()
            mapFragment.getMapAsync { googleMap ->
                googleMap.setOnMapClickListener { latLng ->
                    googleMap.clear()
                    googleMap.addMarker(
                        com.google.android.gms.maps.model.MarkerOptions().position(latLng).title("Pinned Location")
                    )
                    lastPin = latLng
                    // Draw circle for geofence
                    circle = googleMap.addCircle(
                        com.google.android.gms.maps.model.CircleOptions()
                            .center(latLng)
                            .radius(lastRadius.toDouble())
                            .strokeColor(0x5500AAFF)
                            .fillColor(0x2200AAFF)
                    )
                }
                radiusSeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                        lastRadius = progress
                        circle?.radius = progress.toDouble()
                    }
                    override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
                })
                saveButton.setOnClickListener {
                    if (lastPin != null) {
                        // TODO: Save geofence (latLng, lastRadius) to local storage
                        android.widget.Toast.makeText(requireContext(), "Geofence saved!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(requireContext(), "Drop a pin first!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
