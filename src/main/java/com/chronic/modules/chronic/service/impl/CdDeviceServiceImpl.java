package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.DeviceBindDTO;
import com.chronic.modules.chronic.entity.CdDevice;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.mapper.CdDeviceMapper;
import com.chronic.modules.chronic.mapper.CdMeasureMapper;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.service.CdDeviceService;
import com.chronic.modules.chronic.vo.DeviceCardVO;
import com.chronic.modules.miniapp.service.MpAccessService;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备绑定服务实现
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdDeviceServiceImpl implements CdDeviceService {

    /** 常见型号编码 -> 中文名 */
    private static final Map<String, String> MODEL_NAMES = new LinkedHashMap<>();

    static {
        MODEL_NAMES.put("HUAWEI Band 10", "华为手环 10");
        MODEL_NAMES.put("HUAWEI Band 9", "华为手环 9");
        MODEL_NAMES.put("HUAWEI Watch GT 5", "华为手表 GT 5");
        MODEL_NAMES.put("HUAWEI Watch Fit 4", "华为手表 Fit 4");
        MODEL_NAMES.put("HUAWEI Watch 5", "华为手表 5");
    }

    private final CdDeviceMapper deviceMapper;
    private final CdMeasureMapper measureMapper;
    private final CdPatientMapper patientMapper;
    private final MpAccessService mpAccessService;

    @Override
    public List<DeviceCardVO> scan(Long patientId) {
        checkMpPermission(patientId);
        // 未绑定的设备即"附近可发现"，真实场景由 BLE 扫描得到，这里用设备池模拟
        List<CdDevice> list = deviceMapper.selectList(new LambdaQueryWrapper<CdDevice>()
                .isNull(CdDevice::getPatientId)
                .eq(CdDevice::getStatus, "0")
                .orderByAsc(CdDevice::getDeviceId)
                .last("limit 20"));
        List<DeviceCardVO> result = new ArrayList<>();
        for (CdDevice device : list) {
            DeviceCardVO vo = toVO(device);
            vo.setRssi(-40 - (int) (device.getDeviceId() % 25));
            vo.setBattery(device.getBattery() == null ? (int) (60 + device.getDeviceId() % 39) : device.getBattery());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<DeviceCardVO> listByPatient(Long patientId) {
        checkMpPermission(patientId);
        List<CdDevice> list = deviceMapper.selectList(new LambdaQueryWrapper<CdDevice>()
                .eq(CdDevice::getPatientId, patientId)
                .eq(CdDevice::getStatus, "0")
                .orderByDesc(CdDevice::getBindTime));
        List<DeviceCardVO> result = new ArrayList<>();
        for (CdDevice device : list) {
            result.add(toVO(device));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceCardVO bind(DeviceBindDTO dto) {
        checkMpPermission(dto.getPatientId());
        CdPatient patient = patientMapper.selectById(dto.getPatientId());
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }
        CdDevice device = findOrCreate(dto.getBtName());

        if (device.getPatientId() != null && !device.getPatientId().equals(dto.getPatientId())) {
            throw new ServiceException("该设备已被其他患者绑定，请先在原患者下解绑");
        }
        if (dto.getAlias() != null && !dto.getAlias().isBlank()) {
            device.setAlias(dto.getAlias().trim());
        }
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        device.setPatientId(dto.getPatientId());
        device.setPatientName(patient.getName());
        device.setBindUserId(user == null ? null : user.getUserId());
        device.setBindTime(LocalDateTime.now());
        device.setBindSource("1");
        device.setOnlineStatus("1");
        device.setLastOnline(LocalDateTime.now());
        deviceMapper.updateById(device);
        log.info("设备绑定成功：deviceId={} btName={} patientId={}", device.getDeviceId(), device.getDeviceNo(), dto.getPatientId());
        return toVO(deviceMapper.selectById(device.getDeviceId()));
    }

    @Override
    public void unbind(Long deviceId) {
        CdDevice device = requireDevice(deviceId);
        checkMpPermission(device.getPatientId());
        // 解绑需要把字段置空，updateById 会忽略 null，所以用 UpdateWrapper
        deviceMapper.update(null, new LambdaUpdateWrapper<CdDevice>()
                .eq(CdDevice::getDeviceId, deviceId)
                .set(CdDevice::getPatientId, null)
                .set(CdDevice::getPatientName, null)
                .set(CdDevice::getBindUserId, null)
                .set(CdDevice::getBindTime, null)
                .set(CdDevice::getBindSource, null)
                .set(CdDevice::getOnlineStatus, "0")
                .set(CdDevice::getLastOnline, LocalDateTime.now()));
        log.info("设备解绑：deviceId={}", deviceId);
    }

    @Override
    public void updateAlias(Long deviceId, String alias) {
        CdDevice device = requireDevice(deviceId);
        checkMpPermission(device.getPatientId());
        CdDevice update = new CdDevice();
        update.setDeviceId(deviceId);
        update.setAlias(StringUtils.hasText(alias) ? alias.trim() : null);
        deviceMapper.updateById(update);
    }

    @Override
    public PageResult<DeviceCardVO> page(Integer pageNum, Integer pageSize, String keyword, Long patientId) {
        LambdaQueryWrapper<CdDevice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CdDevice::getDeviceNo, keyword)
                    .or().like(CdDevice::getDeviceName, keyword)
                    .or().like(CdDevice::getAlias, keyword)
                    .or().like(CdDevice::getModelName, keyword));
        }
        if (patientId != null) {
            wrapper.eq(CdDevice::getPatientId, patientId);
        }
        wrapper.orderByDesc(CdDevice::getBindTime).orderByAsc(CdDevice::getDeviceId);
        Page<CdDevice> page = deviceMapper.selectPage(
                new Page<>(pageNum == null || pageNum < 1 ? 1 : pageNum,
                        pageSize == null || pageSize < 1 ? 10 : pageSize), wrapper);
        List<DeviceCardVO> rows = new ArrayList<>();
        for (CdDevice device : page.getRecords()) {
            rows.add(toVO(device));
        }
        return PageResult.of(page.getTotal(), rows);
    }

    @Override
    public List<CdMeasure> deviceMeasures(Long deviceId, Integer limit) {
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return measureMapper.selectList(new LambdaQueryWrapper<CdMeasure>()
                .eq(CdMeasure::getDeviceId, deviceId)
                .orderByDesc(CdMeasure::getMeasureTime)
                .last("limit " + size));
    }

    // ------------------------------------------------------------------ 内部方法

    /** 按蓝牙名查找设备，不存在则解析型号后建档 */
    private CdDevice findOrCreate(String btName) {
        String name = btName.trim();
        CdDevice exists = deviceMapper.selectOne(new LambdaQueryWrapper<CdDevice>()
                .eq(CdDevice::getDeviceNo, name)
                .last("limit 1"));
        if (exists != null) {
            return exists;
        }
        ParsedName parsed = parseBtName(name);
        CdDevice device = new CdDevice();
        device.setDeviceNo(name);
        device.setDeviceName(name);
        device.setVendor("HUAWEI");
        device.setModel(parsed.modelCode);
        device.setModelName(parsed.modelName);
        device.setSnSuffix(parsed.suffix);
        device.setDeviceType(parsed.deviceType);
        device.setOnlineStatus("0");
        device.setStatus("0");
        deviceMapper.insert(device);
        log.info("新设备建档：{} -> {}", name, parsed.modelCode);
        return device;
    }

    private CdDevice requireDevice(Long deviceId) {
        CdDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        return device;
    }

    /** 小程序用户走患者权限校验，系统用户（医生）直接放行 */
    private void checkMpPermission(Long patientId) {
        if (patientId == null) {
            return;
        }
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user != null && "mp".equals(user.getUserType())) {
            mpAccessService.requirePatient(patientId);
        }
    }

    private DeviceCardVO toVO(CdDevice device) {
        DeviceCardVO vo = new DeviceCardVO();
        vo.setDeviceId(device.getDeviceId());
        vo.setBtName(device.getDeviceNo());
        vo.setModelCode(device.getModel());
        vo.setModelName(device.getModelName());
        vo.setSnSuffix(device.getSnSuffix());
        vo.setAlias(device.getAlias());
        vo.setDeviceType(device.getDeviceType());
        vo.setVendor(device.getVendor());
        vo.setOnlineStatus(device.getOnlineStatus());
        vo.setBattery(device.getBattery());
        vo.setLastOnline(device.getLastOnline());
        vo.setLastSyncTime(device.getLastSyncTime());
        vo.setBindTime(device.getBindTime());
        vo.setPatientId(device.getPatientId());
        vo.setPatientName(device.getPatientName());
        vo.setBound(device.getPatientId() != null);
        String display = StringUtils.hasText(device.getAlias()) ? device.getAlias()
                : (StringUtils.hasText(device.getModelName()) ? device.getModelName() : device.getDeviceName());
        vo.setDisplayName(display);
        return vo;
    }

    /**
     * 解析蓝牙广播名：HUAWEI Band 10-4A7 -> model=HUAWEI Band 10, suffix=4A7
     */
    private ParsedName parseBtName(String name) {
        ParsedName parsed = new ParsedName();
        String modelPart = name;
        int dash = name.lastIndexOf('-');
        if (dash > 0 && dash < name.length() - 1) {
            String tail = name.substring(dash + 1).trim();
            if (tail.matches("[0-9A-Fa-f]{2,6}")) {
                parsed.suffix = tail.toUpperCase();
                modelPart = name.substring(0, dash).trim();
            }
        }
        String modelCode = modelPart.toUpperCase().startsWith("HUAWEI") ? modelPart : "HUAWEI " + modelPart;
        parsed.modelCode = modelCode;
        parsed.modelName = MODEL_NAMES.getOrDefault(modelCode, modelPart);
        if (modelCode.contains("Band") || modelCode.contains("Watch")) {
            parsed.deviceType = "1";
        } else if (modelCode.contains("血压")) {
            parsed.deviceType = "2";
        } else if (modelCode.contains("血氧")) {
            parsed.deviceType = "3";
        } else if (modelCode.contains("血糖")) {
            parsed.deviceType = "4";
        } else {
            parsed.deviceType = "1";
        }
        return parsed;
    }

    /** 蓝牙名解析结果 */
    private static class ParsedName {
        private String modelCode;
        private String modelName;
        private String suffix;
        private String deviceType;
    }
}
