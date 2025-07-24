#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "android/bitmap.h"
#include <opencv2/opencv.hpp>
#include <cmath>
#include <vector>
#include <map>
#include <filesystem>
#include <fstream>

using namespace cv;
using namespace std;
namespace fs = filesystem;

struct MomentData {
    std::vector<double> huMoments;
    int label;
};

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_imageclassification_MainActivity_getGreetingMessage(JNIEnv* env, jobject) {
    return env->NewStringUTF("Hello from C++");
}

void convertBitmapToMat(JNIEnv * env, jobject bitmap, cv::Mat &output, bool preMultiplyAlpha) {
    AndroidBitmapInfo info;
    void* pixels = nullptr;
    try {
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 || info.format == ANDROID_BITMAP_FORMAT_RGB_565);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);

        output.create(info.height, info.width, CV_8UC4);

        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            cv::Mat temp(info.height, info.width, CV_8UC4, pixels);
            if (preMultiplyAlpha) cvtColor(temp, output, cv::COLOR_mRGBA2RGBA);
            else temp.copyTo(output);
        } else {
            cv::Mat temp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(temp, output, cv::COLOR_BGR5652RGBA);
        }

        AndroidBitmap_unlockPixels(env, bitmap);
    } catch (const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown error during bitmap to mat conversion");
    }
}

std::vector<double> computeHuMoments(const cv::Mat& img) {
    cv::Mat grayImg;
    cv::cvtColor(img, grayImg, cv::COLOR_BGR2GRAY);

    cv::Moments imgMoments = cv::moments(grayImg);
    std::vector<double> hu(7);
    cv::HuMoments(imgMoments, hu);

    return hu;
}

double calcEuclideanDist(const std::vector<double>& a, const std::vector<double>& b) {
    double dist = 0.0;
    for (size_t i = 0; i < a.size(); ++i) {
        dist += std::pow(a[i] - b[i], 2);
    }
    return std::sqrt(dist);
}

std::vector<MomentData> loadHuMomentsFromCSV(const std::string& filePath) {
    std::vector<MomentData> momentsData;
    std::ifstream file(filePath);
    std::string line;

    while (std::getline(file, line)) {
        std::stringstream ss(line);
        std::string token;

        MomentData data;
        bool validLine = true;

        for (int i = 0; i < 7; ++i) {
            std::getline(ss, token, ',');
            try {
                data.huMoments.push_back(std::stod(token));
            } catch (const std::invalid_argument&) {
                validLine = false;
                break;
            }
        }

        if (validLine) {
            std::getline(ss, token, ',');
            try {
                data.label = std::stoi(token);
                momentsData.push_back(data);
            } catch (const std::invalid_argument&) {
                // Invalid label handling
            }
        }
    }

    return momentsData;
}

int identifyImage(const cv::Mat& newImg, const std::vector<MomentData>& dataset) {
    std::vector<double> newImageMoments = computeHuMoments(newImg);
    double minDist = std::numeric_limits<double>::max();
    int label = -1;

    for (const auto& data : dataset) {
        double dist = calcEuclideanDist(newImageMoments, data.huMoments);
        if (dist < minDist) {
            minDist = dist;
            label = data.label;
        }
    }

    return label;
}

extern "C" {
    JNIEXPORT jstring JNICALL
    Java_com_example_imageclassification_MainActivity_recognizeShape(JNIEnv* env, jobject, jobject bitmap, jboolean applyMoment, jstring filePath) {
        cv::Mat img;
        convertBitmapToMat(env, bitmap, img, false);

        const char* path = env->GetStringUTFChars(filePath, nullptr);
        std::vector<MomentData> dataset = loadHuMomentsFromCSV(path);

        int label = identifyImage(img, dataset);

        std::string result;
        switch (label) {
            case 0: result = "Circle"; break;
            case 1: result = "Square"; break;
            case 2: result = "Triangle"; break;
            default: result = "Unknown Shape"; break;
        }

        return env->NewStringUTF(result.c_str());
    }
}
