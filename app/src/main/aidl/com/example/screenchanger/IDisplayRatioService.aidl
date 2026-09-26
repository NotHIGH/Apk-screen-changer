package com.example.screenchanger;

interface IDisplayRatioService {
    String applyRatio(String ratio);
    String resetResolution();
    void destroy() = 16777114;
}