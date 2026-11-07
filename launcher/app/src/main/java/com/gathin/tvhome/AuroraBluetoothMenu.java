package com.gathin.tvhome;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * A remote-friendly Bluetooth quick panel, displayed to the right of the Wi-Fi
 * pill. It does not scan continuously or rely on hidden profile APIs.
 *
 * The Bluetooth power control works directly on PHILCO Android 11 (API 30),
 * where BLUETOOTH_ADMIN allows a user-initiated toggle. On Android 13+
 * apps targeting 33+ may not call enable()/disable(): use system UI instead.
 *
 * Paired devices appear by name. Selecting one opens the vendor Bluetooth
 * settings to connect/disconnect it. Public Android APIs do not offer a
 * universal automatic A2DP/HID connection action for third-party launchers.
 */
final class AuroraBluetoothMenu {
    private final Activity activity;
    private final TextView button;
    private final BluetoothAdapter adapter;
    private final int dp;
    private PopupWindow popup;
    private boolean listening;
    private boolean closed;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                updateButton();
            }
        }
    };

    AuroraBluetoothMenu(Activity activity, TextView button, boolean visible) {
        this.activity = activity;
        this.button = button;
        this.dp = Math.round(activity.getResources().getDisplayMetrics().density * 1f);
        BluetoothAdapter found = null;
        try {
            BluetoothManager manager=(BluetoothManager)activity.getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager != null) found = manager.getAdapter();
        } catch (RuntimeException ignored) { }
        adapter=found;
        button.setVisibility(visible ? View.VISIBLE : View.GONE);
        button.setOnClickListener(v -> showMenu());
        button.setOnLongClickListener(v -> {
            openDeviceSettings();
            return true;
        });
        updateButton();
    }

    void resume() {
        if(closed || button.getVisibility()!=View.VISIBLE) return;
        if(!listening) {
            IntentFilter filter=new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            try {
                if(Build.VERSION.SDK_INT>=33)activity.registerReceiver(
                        receiver, filter, Context.RECEIVER_NOT_EXPORTED);
                else activity.registerReceiver(receiver,filter);
                listening=true;
            } catch(RuntimeException ignored) { }
        }
        updateButton();
    }

    void pause() {
        if(popup!=null) {
            popup.dismiss();
            popup=null;
        }
        if(listening) {
            try {activity.unregisterReceiver(receiver);}catch(RuntimeException ignored){}
            listening=false;
        }
    }

    void close() {
        closed=true;
        pause();
    }

    private int d(float value) {
        return Math.round(value*activity.getResources().getDisplayMetrics().density);
    }

    private boolean permitted() {
        return Build.VERSION.SDK_INT<31
                ||activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                ==PackageManager.PERMISSION_GRANTED;
    }

    private boolean enabled() {
        if(adapter==null || !permitted())return false;
        try { return adapter.isEnabled(); }
        catch(SecurityException | RuntimeException ignored) {return false;}
    }

    private void updateButton() {
        if(button.getVisibility()!=View.VISIBLE)return;
        boolean available=adapter!=null;
        boolean isOn=enabled();
        button.setContentDescription(!available?"Bluetooth indisponível"
                : !permitted()?"Bluetooth: permitir dispositivos próximos"
                : "Bluetooth "+(isOn?"ligado":"desligado"));
        button.setAlpha(!available?0.52f:isOn?1f:0.78f);
    }

    private void requestPermission() {
        if(Build.VERSION.SDK_INT>=31 && !permitted()) {
            activity.requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},741);
        }
    }

    private void changePower() {
        if(adapter==null) {
            Toast.makeText(activity,"Bluetooth não encontrado na TV",Toast.LENGTH_SHORT).show();
            return;
        }
        if(!permitted()) {
            requestPermission();
            return;
        }
        boolean on=enabled();
        try {
            if(!on) {
                if(Build.VERSION.SDK_INT>=33 || !adapter.enable()) {
                    activity.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                }
            } else {
                // Android 13+ disallows disable() for a regular target-34 app.
                // Android 11 TV uses the legacy, user-initiated control.
                if(Build.VERSION.SDK_INT>=33 || !adapter.disable()) {
                    openDeviceSettings();
                }
            }
        } catch(SecurityException | RuntimeException error) {
            openDeviceSettings();
        }
        button.postDelayed(this::updateButton,1100);
    }

    private void openDeviceSettings() {
        try {
            activity.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        } catch(RuntimeException error) {
            try {activity.startActivity(new Intent(Settings.ACTION_SETTINGS));}
            catch(RuntimeException ignored) {
                Toast.makeText(activity,"Configuração Bluetooth indisponível",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private View addRow(LinearLayout rows,String title,String subtitle,
                        Runnable action) {
        View row=activity.getLayoutInflater().inflate(R.layout.item_input,rows,false);
        ((ImageView)row.findViewById(R.id.icon))
                .setImageResource(R.drawable.ic_bluetooth);
        ((TextView)row.findViewById(R.id.title)).setText(title);
        TextView status=row.findViewById(R.id.status);
        if(subtitle!=null && !subtitle.isEmpty()) {
            status.setText(subtitle);
            status.setVisibility(View.VISIBLE);
        } else {
            status.setVisibility(View.GONE);
        }
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        row.setOnClickListener(v -> {
            if(popup!=null)popup.dismiss();
            action.run();
        });
        rows.addView(row);
        return row;
    }

    private void showMenu() {
        if(closed)return;
        if(popup!=null)popup.dismiss();
        View content=activity.getLayoutInflater().inflate(R.layout.popup_inputs,null,false);
        LinearLayout rows=content.findViewById(R.id.rows);
        View first;
        if(adapter==null) {
            first=addRow(rows,"Bluetooth indisponível","Abrir ajustes",
                    this::openDeviceSettings);
        } else if(!permitted()) {
            first=addRow(rows,"Permitir Bluetooth","Dispositivos próximos",
                    this::requestPermission);
        } else {
            boolean on=enabled();
            first=addRow(rows,on?"Desligar Bluetooth":"Ligar Bluetooth",
                    on?"Ligado":"Desligado",this::changePower);

            if(on) {
                try {
                    Set<BluetoothDevice> paired=adapter.getBondedDevices();
                    List<BluetoothDevice> devices=new ArrayList<>(paired);
                    devices.sort(Comparator.comparing(d -> {
                        try {String name=d.getName();return name==null?"":name;}
                        catch(SecurityException ex){return "";}
                    },String.CASE_INSENSITIVE_ORDER));
                    int shown=0;
                    for(BluetoothDevice device:devices) {
                        if(shown++>=5)break;
                        String name;
                        try {name=device.getName();}
                        catch(SecurityException ex){name=null;}
                        if(name==null||name.trim().isEmpty())name="Dispositivo pareado";
                        addRow(rows,name,"Pareado • selecionar",
                                this::openDeviceSettings);
                    }
                    if(devices.isEmpty()) {
                        addRow(rows,"Nenhum dispositivo pareado","Adicionar dispositivo",
                                this::openDeviceSettings);
                    }
                } catch(SecurityException | RuntimeException ignored) {
                    addRow(rows,"Dispositivos Bluetooth","Ver nas configurações",
                            this::openDeviceSettings);
                }
            }
            addRow(rows,"Buscar / gerenciar dispositivos","Configurações da TV",
                    this::openDeviceSettings);
        }
        popup=new PopupWindow(content,d(322),ViewGroup.LayoutParams.WRAP_CONTENT,true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(d(10));
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popup.setOnDismissListener(() -> {
            popup=null;
            button.requestFocus();
            updateButton();
        });
        popup.showAsDropDown(button,0,d(9),Gravity.RIGHT);
        first.requestFocus();
    }
}
