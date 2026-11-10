package br.com.lume35.camera.camera;

import androidx.activity.ComponentActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.view.LifecycleCameraController;
import androidx.core.content.ContextCompat;
import br.com.lume35.camera.R;
import br.com.lume35.camera.ui.VisualPicker;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PhoneLenses {
    private PhoneLenses() {}

    @androidx.annotation.OptIn(markerClass = androidx.camera.camera2.interop.ExperimentalCamera2Interop.class)
    public static void show(ComponentActivity activity, LifecycleCameraController controller,
                            BiConsumer<CameraSelector, String> rear, Consumer<Float> zoomChanged,
                            Consumer<String> message) {
        com.google.common.util.concurrent.ListenableFuture<androidx.camera.lifecycle.ProcessCameraProvider> future =
                androidx.camera.lifecycle.ProcessCameraProvider.getInstance(activity);
        future.addListener(() -> {
            if (activity.isDestroyed()) return;
            try {
                java.util.ArrayList<String> names = new java.util.ArrayList<>();
                java.util.ArrayList<Runnable> choices = new java.util.ArrayList<>();
                for (androidx.camera.core.CameraInfo camera : future.get().getAvailableCameraInfos()) {
                    androidx.camera.camera2.interop.Camera2CameraInfo info =
                            androidx.camera.camera2.interop.Camera2CameraInfo.from(camera);
                    Integer facing = info.getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                    if (facing == null || facing != android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) continue;
                    float[] focal = info.getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    String name = (names.isEmpty() ? activity.getString(R.string.ui_traseira_principal) : activity.getString(R.string.ui_traseira_adicional)) + (focal != null && focal.length > 0
                            ? String.format(Locale.ROOT, activity.getString(R.string.ui_1f_mm), focal[0]) : "");
                    names.add(name);
                    choices.add(() -> rear.accept(camera.getCameraSelector(), name));
                    android.hardware.camera2.CameraManager manager =
                            (android.hardware.camera2.CameraManager) activity.getSystemService(android.content.Context.CAMERA_SERVICE);
                    try {
                        for (String physicalId : manager.getCameraCharacteristics(info.getCameraId()).getPhysicalCameraIds()) {
                            float[] lengths = manager.getCameraCharacteristics(physicalId).get(
                                    android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                            String physicalName = activity.getString(R.string.ui_sensor_traseiro) + (lengths != null && lengths.length > 0
                                    ? String.format(Locale.ROOT, activity.getString(R.string.ui_1f_mm), lengths[0]) : "");
                            CameraSelector physical = new CameraSelector.Builder()
                                    .addCameraFilter(infos -> camera.getCameraSelector().filter(infos))
                                    .setPhysicalCameraId(physicalId).build();
                            names.add(physicalName);
                            choices.add(() -> rear.accept(physical, physicalName));
                        }
                    } catch (android.hardware.camera2.CameraAccessException | IllegalArgumentException ignored) {
                        // Logical camera choices remain usable if physical details are unavailable.
                    }

                }
                androidx.camera.core.ZoomState zoom = controller.getCameraInfo().getZoomState().getValue();
                if (zoom != null) {
                    java.util.TreeSet<Float> ratios = new java.util.TreeSet<>();
                    ratios.add(zoom.getMinZoomRatio());
                    ratios.add(1f);
                    if (zoom.getMaxZoomRatio() >= 2f) ratios.add(2f);
                    if (zoom.getMaxZoomRatio() >= 3f) ratios.add(3f);
                    for (float ratio : ratios) {
                        if (ratio < zoom.getMinZoomRatio() || ratio > zoom.getMaxZoomRatio()) continue;
                        String name = String.format(Locale.ROOT, activity.getString(R.string.ui_zoom_1f_camera_atual), ratio);
                        names.add(name);
                        choices.add(() -> {
                            com.google.common.util.concurrent.ListenableFuture<Void> change = controller.setZoomRatio(ratio);
                            change.addListener(() -> {
                                try { change.get(); zoomChanged.accept(ratio); }
                                catch (Exception error) { message.accept(activity.getString(R.string.ui_nao_foi_possivel_ajustar_o_zoom)); }
                            }, ContextCompat.getMainExecutor(activity));
                        });
                    }
                }
                java.util.ArrayList<VisualPicker.Item> cards=new java.util.ArrayList<>();
                for(String n:names)cards.add(new VisualPicker.Item(n,n.contains("×")?activity.getString(R.string.ui_ampliacao_da_camera_atual_pode_ser_digital):activity.getString(R.string.ui_sensor_disponivel_no_aparelho),10,activity.getColor(R.color.lume_amber)));
                VisualPicker.show(activity,activity.getString(R.string.ui_camera_do_celular),activity.getString(R.string.ui_sensores_fisicos_e_zoom_os_efeitos_classicos_),cards,-1,index -> choices.get(index).run());
            } catch (Exception error) { message.accept(activity.getString(R.string.ui_nao_foi_possivel_consultar_as_lentes)); }
        }, ContextCompat.getMainExecutor(activity));
    }

}
