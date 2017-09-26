package honestdeveloper.photoupload;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ImgDisplayAdapter extends RecyclerView.Adapter<ImgDisplayAdapter.MyViewHolder> {

	private ArrayList<Uri> mImgUriList = new ArrayList<>();

	private Context mContext;

	public ImgDisplayAdapter(Context context, ArrayList<Uri> photoUriList) {
		mContext = context;
		mImgUriList = photoUriList;
	}

	@Override
	public ImgDisplayAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		final View itemView = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.img_row_layout, parent, false);
		return new MyViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(ImgDisplayAdapter.MyViewHolder holder, int position) {
		Glide.with(mContext)
				.load(mImgUriList.get(position))
				.into(holder.mImage);
	}

	@Override
	public int getItemCount() {
		Log.v(Constants.TAG, String.valueOf(mImgUriList.size()));
		return mImgUriList.size();
	}

	public class MyViewHolder extends RecyclerView.ViewHolder {

		ImageView mImage;

		public MyViewHolder(View view) {
			super(view);
			mImage = (ImageView) view.findViewById(R.id.image);
		}
	}
}
