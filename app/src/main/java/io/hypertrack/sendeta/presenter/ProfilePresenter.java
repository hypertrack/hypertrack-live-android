/*
The MIT License (MIT)

Copyright (c) 2015-2017 HyperTrack (http://hypertrack.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package io.hypertrack.sendeta.presenter;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.hypertrack.lib.HyperTrack;
import com.hypertrack.lib.callbacks.HyperTrackCallback;
import com.hypertrack.lib.models.ErrorResponse;
import com.hypertrack.lib.models.SuccessResponse;

import java.io.File;

import io.hypertrack.sendeta.callback.OnOnboardingImageUploadCallback;
import io.hypertrack.sendeta.model.HyperTrackLiveUser;
import io.hypertrack.sendeta.store.OnboardingManager;
import io.hypertrack.sendeta.view.Profile;
import io.hypertrack.sendeta.view.ProfileView;

/**
 * Created by suhas on 24/02/16.
 */
public class ProfilePresenter implements IProfilePresenter<ProfileView> {

    private static final String TAG = Profile.class.getSimpleName();
    private ProfileView view;
    private OnboardingManager onboardingManager = OnboardingManager.sharedManager();

    @Override
    public void attachView(ProfileView view) {
        this.view = view;
        HyperTrackLiveUser user = this.onboardingManager.getUser();
        this.view.updateViews(user.getName(), user.getPhone(), user.getCountryCode(), user.getPhoto());
    }

    @Override
    public void detachView() {
        view = null;
    }

    @Override
    public void attemptLogin(final String userName, String phone, String ISOCode, final String deviceID, final File profileImage,
                             final Bitmap oldProfileImage, final Bitmap updatedProfileImage) {
        final HyperTrackLiveUser user = this.onboardingManager.getUser();

        // Update Country Code from device's current location
        if (!TextUtils.isEmpty(ISOCode))
            user.setCountryCode(ISOCode);

        // Set user's profile image
        if (profileImage != null && profileImage.length() > 0) {
            user.setPhotoImage(profileImage);
        }

        HyperTrackLiveUser.setHyperTrackLiveUser();

        try {
            HyperTrack.createUser(userName, user.getInternationalNumber(phone), user.getInternationalNumber(phone) + "_" + deviceID,
                    new HyperTrackCallback() {
                @Override
                public void onSuccess(@NonNull SuccessResponse successResponse) {

                    if (profileImage != null && profileImage.length() > 0) {
                        onboardingManager.uploadPhoto(oldProfileImage, updatedProfileImage, new OnOnboardingImageUploadCallback() {
                            @Override
                            public void onSuccess() {
                                if (view != null) {
                                    view.showProfilePicUploadSuccess();
                                    view.navigateToHomeScreen();
                                }
                            }

                            @Override
                            public void onError() {
                                Log.i(TAG, "Profile Image not saved in local database");
                                if (view != null) {
                                    view.showProfilePicUploadError();
                                }
                            }

                            @Override
                            public void onImageUploadNotNeeded() {
                            }
                        });
                    } else {
                        if (view != null) {
                            view.navigateToHomeScreen();
                        }
                    }
                }

                @Override
                public void onError(@NonNull ErrorResponse errorResponse) {
                    if (view != null) {
                        view.showErrorMessage();
                    }

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (view != null) {
                view.showErrorMessage();
            }
        }
    }
}
