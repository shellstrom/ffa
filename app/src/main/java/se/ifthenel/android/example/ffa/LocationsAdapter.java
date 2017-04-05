package se.ifthenel.android.example.ffa;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

/**
 * This adapter holds and displays views containing information for each row of the Locations list
 */
class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.ViewHolder> implements LOG {
  private final List<Location> mLocations;

  LocationsAdapter(List<Location> locations) {
    mLocations = locations;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.locations_listview_row, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    if(mLocations != null) {
      holder.textTitle.setText(mLocations.get(position).getLocationName());
      // If there is no plan image associated with this location, hide the plan image pin
      if(mLocations.get(position).getPlanImage().isEmpty()) {
        holder.imageLocation.setVisibility(ImageButton.INVISIBLE);
      } else {
        holder.imageLocation.setVisibility(ImageButton.VISIBLE);
      }
    }
  }

  @Override
  public int getItemCount() {
    if(mLocations != null) {
      return mLocations.size();
    } else {
      return 0;
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    View itemView;

    public int id;
    TextView textTitle;
    ImageButton imageLocation;

    ViewHolder(View view) {
      super(view);
      itemView = view;
      textTitle = (TextView) view.findViewById(R.id.text_location_title);
      imageLocation = (ImageButton) view.findViewById(R.id.imagebutton_location);

      // When clicking a row, show the maps activity
      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Location location = mLocations.get(getAdapterPosition());
          Intent intent = new Intent(view.getContext(), MapsActivity.class);
          intent.putExtra("location", location);
          view.getContext().startActivity(intent);
        }
      });

      // When clicking the location pin, show the plan activity
      imageLocation.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Location location = mLocations.get(getAdapterPosition());
          Intent intent = new Intent(view.getContext(), PlanActivity.class);
          intent.putExtra("location", location);
          view.getContext().startActivity(intent);
        }
      });
    }
  }
}