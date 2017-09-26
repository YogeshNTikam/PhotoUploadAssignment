package honestdeveloper.photoupload;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static honestdeveloper.photoupload.Constants.PERMISSIONS_REQUEST_CAMERA;
import static honestdeveloper.photoupload.Constants.PERMISSIONS_REQUEST_WRITE_STORAGE;
import static honestdeveloper.photoupload.Constants.REQUEST_IMAGE_CAPTURE;
import static honestdeveloper.photoupload.Constants.REQUEST_IMAGE_SELECTION;
import static honestdeveloper.photoupload.Constants.TAG;

/**
 * Single Activity irrespective of the user choice.
 * Handles both, Multiple selection from Gallery and capture of Images.
 * Couple of permissions are requested in tandem with Android conventions.
 */

public class ImageProcessingActivity extends AppCompatActivity implements
		GoogleApiClient.OnConnectionFailedListener {

	private String mCurrentPhotoPath;
	private ArrayList<Uri> mPhotoUriList = new ArrayList<>();
	private Button mOperationBtn;
	private RecyclerView mImgDisplayRecycler;
	private boolean mIsCameraOptionSelected = false;
	private Context mContext;
	private ImgDisplayAdapter mImgDisplayAdapter;
	private RecyclerView.LayoutManager mLayoutManager;
	private GoogleApiClient mGoogleApiClient;
	private Button mUploadImgBtn;
	private Bitmap mBitmapToSave;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_processing);

		init();
		setListeners();
	}

	private void init() {

		mContext = ImageProcessingActivity.this;

		mOperationBtn = (Button) findViewById(R.id.operation_btn);
		mImgDisplayRecycler = (RecyclerView) findViewById(R.id.img_display_recycler);
		mUploadImgBtn = (Button) findViewById(R.id.upload_img);

		if (getIntent().getExtras() != null) {
			if (getIntent().getBooleanExtra(Constants.EXTRA_CAMERA_SELECTED, false)) {
				mIsCameraOptionSelected = true;
				mOperationBtn.setText(getString(R.string.camera_string));
			}
		} else {
			mIsCameraOptionSelected = false;
			mOperationBtn.setText(getString(R.string.gallery_string));
		}

		initGoogleApiClient();
	}

	private void initGoogleApiClient() {
		// Build a GoogleApiClient with access to the GoogleDrive API.
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.enableAutoManage(this, this)
				.addApi(Drive.API)
				.addScope(Drive.SCOPE_FILE)
				.build();
	}

	private void setListeners() {
		mOperationBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mIsCameraOptionSelected) {
					checkCameraPermission();
				} else {
					checkStoragePermission();
				}
			}
		});

		mUploadImgBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mGoogleApiClient != null) {
					mGoogleApiClient.connect();
					saveFileToDrive();
				}
			}
		});
	}

	private void checkCameraPermission() {
		if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {

			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.CAMERA)) {

				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
						PERMISSIONS_REQUEST_CAMERA);

				Log.v(TAG, "Coming into Rationale");
			} else {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
						PERMISSIONS_REQUEST_CAMERA);

				Log.v(TAG, "Coming into Non Rationale");
			}
		} else {
			captureImgIntent();
		}
	}

	private void checkStoragePermission() {
		if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						PERMISSIONS_REQUEST_WRITE_STORAGE);

				Log.v(TAG, "Coming into Rationale");
			} else {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						PERMISSIONS_REQUEST_WRITE_STORAGE);

				Log.v(TAG, "Coming into Non Rationale");
			}
		} else {
			selectImagesFromGallery();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		switch (requestCode) {

			case PERMISSIONS_REQUEST_CAMERA: {
				if (grantResults.length > 0 && grantResults[0]
						== PackageManager.PERMISSION_GRANTED) {
					Log.v(TAG, "Camera permission granted");
					captureImgIntent();
				} else {
					Log.v(TAG, "Camera permission not granted");
					Toast.makeText(this, "Please provide permission to access Camera",
							Toast.LENGTH_LONG).show();
				}
			}

			case PERMISSIONS_REQUEST_WRITE_STORAGE: {
				if (grantResults.length > 0 && grantResults[0]
						== PackageManager.PERMISSION_GRANTED) {
					Log.v(TAG, "Storage permission granted");
					selectImagesFromGallery();
				} else {
					Log.v(TAG, "Storage permission not granted");
					Toast.makeText(this, "Please provide permission to access External Storage",
							Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	private void captureImgIntent() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			// Create the File where the photo should go
			File photoFile = null;
			try {
				photoFile = createImageFile();

			} catch (IOException ex) {
				// Error occurred while creating the File

			}
			if (photoFile != null) {
				Uri photoURI = FileProvider.getUriForFile(this,
						"honestdeveloper.photoupload",
						photoFile);
				mPhotoUriList.add(Uri.fromFile(photoFile));

				Log.v("Photo File : ", photoFile.toString());
				Log.v("PhotoUri File : ", Uri.fromFile(photoFile).toString());
				Log.v("PhotoUri Provider : ", photoURI.toString());

				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
				startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
			}
		}
	}

	private void selectImagesFromGallery() {
		Intent chooseIntent = new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		chooseIntent.setType("image/*");
		chooseIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		startActivityForResult(chooseIntent, REQUEST_IMAGE_SELECTION);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
			// Store the image data as a bitmap for writing later.
			mBitmapToSave = (Bitmap) data.getExtras().get("data");
			galleryAddPic();
			mUploadImgBtn.setVisibility(View.VISIBLE);
			setRecyclerView();
		} else if (requestCode == REQUEST_IMAGE_SELECTION && resultCode == RESULT_OK) {

			// Get the Image from data
			String[] projection = {MediaStore.Images.Media.DATA};
			if (data.getData() != null) {
				Uri imageUri = data.getData();
				Log.v("Gallery PhotoUri 1: ", imageUri.toString());
				loadImagesFromGallery(imageUri, projection);
			} else {
				if (data.getClipData() != null) {
					ClipData mClipData = data.getClipData();
					ArrayList<Uri> arrayUri = new ArrayList<Uri>();
					for (int i = 0; i < mClipData.getItemCount(); i++) {

						ClipData.Item item = mClipData.getItemAt(i);
						Uri uri = item.getUri();
						arrayUri.add(uri);
						Log.v("Gallery PhotoUri 2: ", uri.toString());

						loadImagesFromGallery(uri, projection);
					}
					Log.v(TAG, "Selected Images" + arrayUri.size());
				}
			}
		} else if (requestCode == Constants.REQUEST_CODE_CREATOR && resultCode == RESULT_OK) {
			Log.v(TAG, "Image successfully saved.");
			Toast.makeText(mContext, "Image saved to drive", Toast.LENGTH_LONG).show();
			mBitmapToSave = null;
		}
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";
		File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		File image = File.createTempFile(
				imageFileName,  /* prefix */
				".jpg",         /* suffix */
				storageDir      /* directory */
		);
		mCurrentPhotoPath = image.getAbsolutePath();
		return image;
	}

	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		File f = new File(mCurrentPhotoPath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		this.sendBroadcast(mediaScanIntent);
	}

	/**
	 * Ideally , for the following function,
	 * should be using AsyncTask or Loaders in actual application
	 */
	private void loadImagesFromGallery(Uri uri, String[] filePathColumn) {

		String imgEncoded = null;
		Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			imgEncoded = cursor.getString(columnIndex);
			cursor.close();
		}
		if (imgEncoded != null) {
			File file = new File(imgEncoded);
			if (file.exists()) {
				Log.v("Gallery Photo Uri File", Uri.fromFile(file).toString());
				Uri fileUri = Uri.fromFile(file);
				mPhotoUriList.add(fileUri);
				mUploadImgBtn.setVisibility(View.VISIBLE);

				setRecyclerView();

				/**
				 * For image Uploading
				 */
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				try {
					mBitmapToSave = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void setRecyclerView() {
		mImgDisplayRecycler.setVisibility(View.VISIBLE);
		mLayoutManager = new LinearLayoutManager(this);
		mImgDisplayRecycler.setLayoutManager(mLayoutManager);
		mImgDisplayAdapter = new ImgDisplayAdapter(mContext, mPhotoUriList);
		mImgDisplayRecycler.setAdapter(mImgDisplayAdapter);
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		// An unresolvable error has occurred and Google APIs (including Sign-In) will not
		// be available.
		Log.v(TAG, "onConnectionFailed:" + connectionResult);
		if (!connectionResult.hasResolution()) {
			GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
			return;
		}
	}

	/**
	 * Create a new file and save(upload) it to Drive.
	 */
	private void saveFileToDrive() {
		final Bitmap image = mBitmapToSave;
		Drive.DriveApi.newDriveContents(mGoogleApiClient)
				.setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

					@Override
					public void onResult(DriveApi.DriveContentsResult result) {
						// Operation not successful
						if (!result.getStatus().isSuccess()) {
							Log.v(TAG, "Failed to create new contents.");
							return;
						}
						// Otherwise, we can write our data to the new contents.
						Log.v(TAG, "New contents created.");
						// Get an output stream for the contents.
						OutputStream outputStream = result.getDriveContents().getOutputStream();
						// Write the bitmap data from it.
						ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
						image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
						try {
							outputStream.write(bitmapStream.toByteArray());
						} catch (IOException e1) {
							Log.v(TAG, "Unable to write file contents.");
						}
						// Create the initial metadata - MIME type and title.
						// Note that the user will be able to change the title later.
						MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
								.setMimeType("image/jpeg").setTitle("Android Photo.png").build();
						// Create an intent for the file chooser, and start it.
						IntentSender intentSender = Drive.DriveApi
								.newCreateFileActivityBuilder()
								.setInitialMetadata(metadataChangeSet)
								.setInitialDriveContents(result.getDriveContents())
								.build(mGoogleApiClient);
						try {
							startIntentSenderForResult(
									intentSender, Constants.REQUEST_CODE_CREATOR, null, 0, 0, 0);
						} catch (IntentSender.SendIntentException e) {
							Log.i(TAG, "Failed to launch file chooser.");
						}
					}
				});
	}
}


