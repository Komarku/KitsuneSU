
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.18.1)

project("kernelsu")

find_package(cxx REQUIRED CONFIG)
link_libraries(cxx::cxx)

add_library(kernelsu
        SHARED
        jni.cc
        ksu.cc
        )

target_include_directories(kernelsu PRIVATE . ${CMAKE_CURRENT_SOURCE_DIR}/../../../../..)

find_library(log-lib log)

target_link_libraries(kernelsu ${log-lib})

add_library(adbroot SHARED adbroot.cc)
target_link_libraries(adbroot ${log-lib})

add_library(apjni
        SHARED
        apjni/apjni.cpp
        apjni/security.cpp
        )
target_include_directories(apjni PRIVATE apjni)
target_compile_definitions(apjni PRIVATE MAJOR=0 MINOR=13 PATCH=9)
target_compile_options(apjni PRIVATE -Wno-vla-cxx-extension)
target_link_libraries(apjni ${log-lib})
