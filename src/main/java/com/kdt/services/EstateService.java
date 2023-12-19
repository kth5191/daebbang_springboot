package com.kdt.services;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kdt.domain.entities.Estate;
import com.kdt.domain.entities.EstateImage;
import com.kdt.domain.entities.UploadEstate;
import com.kdt.domain.entities.UploadEstateOption;
import com.kdt.dto.EstateDTO;
import com.kdt.dto.UploadEstateDTO;
import com.kdt.dto.UploadEstateOptionDTO;
import com.kdt.mappers.EstateMapper;
import com.kdt.mappers.UploadEstateMapper;
import com.kdt.mappers.UploadEstateOptionMapper;
import com.kdt.repositories.EstateImageRepository;
import com.kdt.repositories.EstateRepository;
import com.kdt.repositories.UploadEstateOptionRepository;
import com.kdt.repositories.UploadEstateRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Service
public class EstateService {

	@Autowired
	private UploadEstateMapper ueMapper;
	@Autowired
	private UploadEstateOptionMapper ueoMapper;
	@Autowired
	private EstateMapper eMapper;

	@Autowired
	private UploadEstateRepository ueRepo;
	@Autowired
	private UploadEstateOptionRepository ueoRepo;
	@Autowired
	private EstateImageRepository eiRepo;
	@Autowired
	private EstateRepository eRepo;

	@Transactional
	public void insertEstate(UploadEstateDTO dto, List<UploadEstateOptionDTO> optionDTOList,
			List<MultipartFile> images) {
		UploadEstate estate = ueMapper.toEntity(dto);

		// 보증금 입력 안 했으면 0
		if (estate.getDeposit() == null) {
			estate.setDeposit(0L);
		}

		// 관리비 입력 안 했으면 0
		if (estate.getMaintenanceCost() == null) {
			estate.setMaintenanceCost(0L);
		}
		// 작성일 입력
		estate.setWriteDate(new Timestamp(System.currentTimeMillis()));

		try {
			// 매물 입력
			Long parentSeq = ueRepo.save(estate).getEstateId();

			// 매물 옵션 입력 ->
			for (UploadEstateOptionDTO optionDTO : optionDTOList) {
				UploadEstateOption option = ueoMapper.toEntity(optionDTO);
				option.setEstateCode(parentSeq);

				ueoRepo.save(option);
			}
			// <- 매물 옵션 입력

			// 사진 파일 입력 ->
			if (images.size() != 0) {
				String upload = "c:/uploads/estateImages/";
				File uploadPath = new File(upload);
				if (!uploadPath.exists()) {
					uploadPath.mkdir();
				}

				for (MultipartFile image : images) {
					String oriName = image.getOriginalFilename();
					String sysName = UUID.randomUUID() + "_" + oriName;

					image.transferTo(new File(uploadPath, sysName));
					eiRepo.save(new EstateImage(null, oriName, sysName, parentSeq));
				}
			}
			// <- 사진 파일 입력
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public List<EstateDTO> selectById(String loginId) {

		List<Estate> eList = eRepo.findAllByRealEstateAgentEmail(loginId);
		
		List<EstateDTO> list = eMapper.toDtoList(eList);

		return list;
	}

	public List<EstateDTO> selectAll() {
		List<Estate> eList = eRepo.findAll();
		List<EstateDTO> list = eMapper.toDtoList(eList);

		return list;
	}

	public UploadEstateDTO selectById(Long estateId) {
		UploadEstate estate = ueRepo.findById(estateId).get();
		UploadEstateDTO dto = ueMapper.toDto(estate);

		return dto;
	}

	public EstateDTO getById(Long estateId) {
		Estate estate = eRepo.findById(estateId).get();
		EstateDTO dto = eMapper.toDto(estate);

		return dto;
	}

	@Transactional
	public void deleteById(Long estateId) {

		try {
			// 매물 옵션 정보 삭제
			ueoRepo.deleteByEstateCode(estateId);

			// 사진 파일 삭제 ->
			// 실제로 지울 파일 이름 검색
			List<EstateImage> eiList = eiRepo.findAllByParentId(estateId);
			List<String> delFileList = new ArrayList<>();
			for (EstateImage image : eiList) {
				delFileList.add(image.getSysName());
			}
			delServerFile(delFileList);
			// DB에서 삭제
			eiRepo.deleteByParentId(estateId);
			// <- 사진 파일 삭제

			// 매물 정보 삭제
			ueRepo.deleteById(estateId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 사진 파일 삭제
	public void delServerFile(List<String> delFileList) throws Exception {
		String filePath = "C:/uploads";
		File uploadFilePath = new File(filePath);
		if (!uploadFilePath.exists()) {
			uploadFilePath.mkdir();
		}

		String realPath = "C:/uploads/estateImages/";
		File uploadPath = new File(realPath);
		if (!uploadPath.exists()) {
			uploadPath.mkdir();
		}

		if (delFileList != null) {
			for (String delFile : delFileList) {
				if (delFile != null) {
					Path path = Paths.get(uploadPath + "/" + delFile);
					java.nio.file.Files.deleteIfExists(path);
				}
			}
		}
	}

	// 매물 정보 수정
	@Transactional
	public void updateById(UploadEstateDTO dto, List<UploadEstateOptionDTO> optionDTOList, List<MultipartFile> images) {
		Long estateId = dto.getEstateId();

		UploadEstate estate = ueRepo.findById(estateId).get();

		// 보증금 입력 안 했으면 0
		if (dto.getDeposit() == null) {
			dto.setDeposit(0L);
		}

		// 관리비 입력 안 했으면 0
		if (dto.getMaintenanceCost() == null) {
			dto.setMaintenanceCost(0L);
		}

		// 이미지 파일 수정한 경우
		if (images != null) {
			try {
				// 매물 옵션 정보 삭제
				ueoRepo.deleteByEstateCode(estateId);

				// 사진 파일 삭제 ->
				// 실제로 지울 파일 이름 검색
				List<EstateImage> eiList = eiRepo.findAllByParentId(estateId);
				List<String> delFileList = new ArrayList<>();
				for (EstateImage image : eiList) {
					delFileList.add(image.getSysName());
				}
				delServerFile(delFileList);
				// DB에서 삭제
				eiRepo.deleteByParentId(estateId);
				// <- 사진 파일 삭제

				// 매물 옵션 입력 ->
				for (UploadEstateOptionDTO optionDTO : optionDTOList) {
					UploadEstateOption option = ueoMapper.toEntity(optionDTO);
					option.setEstateCode(estateId);

					ueoRepo.save(option);
				}
				// <- 매물 옵션 입력

				// 사진 파일 입력 ->
				if (images.size() != 0) {
					String upload = "c:/uploads/estateImages/";
					File uploadPath = new File(upload);
					if (!uploadPath.exists()) {
						uploadPath.mkdir();
					}

					for (MultipartFile image : images) {
						String oriName = image.getOriginalFilename();
						String sysName = UUID.randomUUID() + "_" + oriName;

						image.transferTo(new File(uploadPath, sysName));
						eiRepo.save(new EstateImage(null, oriName, sysName, estateId));
					}
				}
				// <- 사진 파일 입력

				// 매물 정보 업데이트
				ueMapper.updateEntityFromDTO(dto, estate);
				ueRepo.save(estate);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// 이미지 파일 수정하지 않은 경우
		else {
			try {
				// 매물 옵션 정보 삭제
				ueoRepo.deleteByEstateCode(estateId);
				// 매물 옵션 입력 ->
				for (UploadEstateOptionDTO optionDTO : optionDTOList) {
					UploadEstateOption option = ueoMapper.toEntity(optionDTO);
					option.setEstateCode(estateId);

					ueoRepo.save(option);
				}
				// <- 매물 옵션 입력
				ueMapper.updateEntityFromDTO(dto, estate);
				ueRepo.save(estate);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// 마이페이지 매물 불러오기
	public EstateDTO selectEstate(Long id) {
		Estate e = eRepo.findById(id).get();
		EstateDTO edto = eMapper.toDto(e);
		return edto;
	}
}
