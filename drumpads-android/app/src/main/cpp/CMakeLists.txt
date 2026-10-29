cmake_minimum_required(VERSION 3.22.1)
project("drumpads_audio")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(${PROJECT_NAME} SHARED
    native-lib.cpp
    AudioEngine.cpp
)

find_package(oboe QUIET CONFIG)

target_include_directories(${PROJECT_NAME} PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

if (TARGET oboe::oboe)
    target_link_libraries(${PROJECT_NAME}
        PRIVATE
        oboe::oboe
        log
    )
else()
    target_link_libraries(${PROJECT_NAME}
        PRIVATE
        log
    )
endif()
