import { useEffect } from "react";
import { App as CapacitorApp } from "@capacitor/app";
import { Capacitor } from "@capacitor/core";
import { SplashScreen } from "@capacitor/splash-screen";
import { StatusBar, Style } from "@capacitor/status-bar";

export default function NativeAppBridge() {
  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return undefined;

    document.documentElement.classList.add("capacitor-native");
    void StatusBar.setStyle({ style: Style.Dark });
    void SplashScreen.hide();

    let backButtonListener;
    void CapacitorApp.addListener("backButton", ({ canGoBack }) => {
      if (canGoBack) {
        window.history.back();
        return;
      }
      void CapacitorApp.exitApp();
    }).then((listener) => {
      backButtonListener = listener;
    });

    return () => {
      document.documentElement.classList.remove("capacitor-native");
      void backButtonListener?.remove();
    };
  }, []);

  return null;
}
