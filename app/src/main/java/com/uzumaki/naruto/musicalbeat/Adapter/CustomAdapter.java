package com.uzumaki.naruto.musicalbeat.Adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.uzumaki.naruto.musicalbeat.R;
import com.uzumaki.naruto.musicalbeat.Model.SongInfo;

import java.util.ArrayList;

/**
 * Created by Bhavik Bist on 15-07-2017.
 */

public class CustomAdapter extends BaseAdapter {
    private static final String TAG = "CustomAdapter";

    private Context mContext;
    private ArrayList<SongInfo> songInfos;

    public CustomAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void update(ArrayList<SongInfo> songInfos) {
        this.songInfos = songInfos;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return songInfos != null ? songInfos.size() : 0;
    }

    @Override
    public Object getItem(int pos) {
        return songInfos.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return 0;
    }

    private class Holder {
        TextView tv_song_name, tv_song_artist, tv_song_duration;
    }

    @Override
    public View getView(final int pos, View view, ViewGroup viewGroup) {
        final Holder holder;
        if (view == null) {
            holder = new Holder();
            //LayoutInflater.from(mContext);
            LayoutInflater inflater = (LayoutInflater) ((Activity) mContext).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.music_list_item, viewGroup, false);

            holder.tv_song_artist = (TextView) view.findViewById(R.id.tv_song_artist);
            holder.tv_song_name = (TextView) view.findViewById(R.id.tv_song_name);
            holder.tv_song_name.setSelected(true);      // for marquee trick
            holder.tv_song_duration = (TextView) view.findViewById(R.id.tv_song_duration);

            view.setTag(holder);
        } else
            holder = (Holder) view.getTag();

        final SongInfo songInfo = songInfos.get(pos);

        holder.tv_song_name.setText(songInfo.getSongName());
        holder.tv_song_artist.setText(songInfo.getArtistName());
        holder.tv_song_duration.setText(songInfo.getDuration());

        return view;
    }
}
