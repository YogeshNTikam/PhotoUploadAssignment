package honestdeveloper.photoupload;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

	private Button mCameraSelectBtn;
	private Button mGallerySelectBtn;

	private boolean mIsCameraSelected = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		init();
		setListeners();
	}

	private void init() {

		mCameraSelectBtn = (Button) findViewById(R.id.img_camera_btn);
		mGallerySelectBtn = (Button) findViewById(R.id.img_gallery_btn);
	}

	private void setListeners() {

		mCameraSelectBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mIsCameraSelected = true;
				startImageProcessingActivity();

			}
		});

		mGallerySelectBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mIsCameraSelected = false;
				startImageProcessingActivity();
			}
		});
	}

	private void startImageProcessingActivity() {
		Intent imgIntent = new Intent(this, ImageProcessingActivity.class);
		if (mIsCameraSelected) {
			imgIntent.putExtra(Constants.EXTRA_CAMERA_SELECTED, true);
		}
		startActivity(imgIntent);
	}
}