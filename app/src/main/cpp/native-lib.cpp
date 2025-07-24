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

struct HuMomentsData {
    std::vector<double> hu_moments;
    int label;
};



extern "C" JNIEXPORT jstring JNICALL
Java_com_logic_reconocimientodeformas_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


void bitmapToMat(JNIEnv * env, jobject bitmap, cv::Mat &dst, jboolean needUnPremultiplyAlpha) {
    AndroidBitmapInfo info;
    void* pixels = 0;
    try {
        // Verificar información del bitmap
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 || info.format == ANDROID_BITMAP_FORMAT_RGB_565);

        // Bloquear píxeles
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);

        dst.create(info.height, info.width, CV_8UC4); // Crear matriz de destino

        // Procesar según formato
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if (needUnPremultiplyAlpha) cvtColor(tmp, dst, cv::COLOR_mRGBA2RGBA);
            else tmp.copyTo(dst);
        } else { // RGB_565
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(tmp, dst, cv::COLOR_BGR5652RGBA);
        }

        // Liberar los píxeles
        AndroidBitmap_unlockPixels(env, bitmap);
    } catch (const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nBitmapToMat}");
    }
}

std::vector<double> calculate_hu_moments(const cv::Mat& image) {
    // Convertir la imagen a escala de grises
    cv::Mat gray_image;
    cv::cvtColor(image, gray_image, cv::COLOR_BGR2GRAY);

    // Calcular los momentos de la imagen
    cv::Moments moments = cv::moments(gray_image);

    // Calcular los momentos invariantes de Hu
    std::vector<double> hu_moments(7);
    cv::HuMoments(moments, hu_moments);

    return hu_moments;
}

// Función para calcular la distancia Euclidiana
double euclidean_distance(const std::vector<double>& a, const std::vector<double>& b) {
    double sum = 0.0;
    for (size_t i = 0; i < a.size(); ++i) {
        sum += std::pow(a[i] - b[i], 2);
    }
    return std::sqrt(sum);
}

std::vector<HuMomentsData> read_hu_moments_from_csv(const std::string& filename) {
    std::vector<HuMomentsData> hu_data;
    std::ifstream file(filename);
    std::string line;

    // Leer cada línea del archivo CSV
    while (std::getline(file, line)) {
        std::stringstream ss(line);
        std::string token;

        HuMomentsData data;
        bool valid_line = true;

        // Leer los momentos de Hu (7 valores)
        for (int i = 0; i < 7; ++i) {
            std::getline(ss, token, ',');
            try {
                data.hu_moments.push_back(std::stod(token));  // Convierte el token a un double
            } catch (const std::invalid_argument& e) {
                std::cerr << "Error al convertir el valor de momento: " << token << std::endl;
                valid_line = false;  // Marca la línea como inválida
                break;
            }
        }

        // Leer la etiqueta
        if (valid_line) {
            std::getline(ss, token, ',');
            try {
                data.label = std::stoi(token);  // Convierte el token a un int
                hu_data.push_back(data);
            } catch (const std::invalid_argument& e) {
                std::cerr << "Error al convertir la etiqueta: " << token << std::endl;
            }
        }
    }

    return hu_data;
}

// Clasificar la nueva imagen basándonos en los momentos de Hu
int classify_image(const cv::Mat& new_image, const std::vector<HuMomentsData>& dataset) {
    // Calcular los momentos de Hu para la nueva imagen
    std::vector<double> new_hu_moments = calculate_hu_moments(new_image);

    // Inicializar la distancia mínima y la etiqueta predicha
    double min_distance = std::numeric_limits<double>::max();
    int predicted_label = -1;

    // Comparar la nueva imagen con todas las imágenes en el dataset
    for (const auto& data : dataset) {
        double dist = euclidean_distance(new_hu_moments, data.hu_moments);
        if (dist < min_distance) {
            min_distance = dist;
            predicted_label = data.label;
        }
    }

    return predicted_label;
}



extern "C"{

    JNIEXPORT jstring JNICALL Java_com_logic_reconocimientodeformas_MainActivity_reconocimiento(JNIEnv * env, jobject /**/, jobject bIn, jboolean momento, jstring file){
        Mat input;
        bitmapToMat(env, bIn, input, false);

        //cvtColor(input, input, IMREAD_GRAYSCALE);

        const char* files = env->GetStringUTFChars(file, nullptr);

        std::vector<HuMomentsData> dataset = read_hu_moments_from_csv(files);

        int predicted_label = classify_image(input, dataset);

        string predictedLabel;
        switch (predicted_label) {
            case 0: predictedLabel = "Circulo"; break;
            case 1: predictedLabel = "Cuadrado"; break;
            case 2: predictedLabel = "Triangulo"; break;
        }

        return env->NewStringUTF(predictedLabel.c_str());

    }
}
