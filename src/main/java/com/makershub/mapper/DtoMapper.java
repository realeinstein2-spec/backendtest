package com.makershub.mapper;

import com.makershub.dto.response.*;
import com.makershub.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DtoMapper {

    @Mapping(target = "role", source = "role")
    @Mapping(target = "otpCode", ignore = true)
    AuthResponse.UserSummaryResponse toUserSummary(User user);

    @Mapping(target = "smeId", source = "sme.id")
    @Mapping(target = "smeName", source = "sme.fullName")
    @Mapping(target = "productImageUrls", source = "attachmentUrls")
    JobResponse.JobDetailResponse toJobResponse(JobListing job);

    @Mapping(target = "jobId", source = "jobListing.id")
    @Mapping(target = "factoryId", source = "factory.id")
    @Mapping(target = "factoryName", source = "factory.companyName")
    @Mapping(target = "factorySectorTags", source = "factory.sectorTags")
    @Mapping(target = "factoryLogoUrl", source = "factory.user.profileImageUrl")
    @Mapping(target = "factoryRating", source = "factory.user.ratingAvg")
    @Mapping(target = "factoryLatitude", source = "factory.latitude")
    @Mapping(target = "factoryLongitude", source = "factory.longitude")
    @Mapping(target = "factoryAddress", source = "factory.address")
    BidResponse.BidDetailResponse toBidResponse(Bid bid);

    @Mapping(target = "jobId", source = "jobListing.id")
    @Mapping(target = "bidId", source = "bid.id")
    @Mapping(target = "smeId", source = "sme.id")
    @Mapping(target = "factoryId", source = "factory.id")
    @Mapping(target = "factoryName", expression = "java(order.getFactory().getFactory() != null ? order.getFactory().getFactory().getCompanyName() : null)")
    @Mapping(target = "smeName", source = "sme.fullName")
    OrderResponse.OrderDetailResponse toOrderResponse(Order order);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    @Mapping(target = "reviewedId", source = "reviewed.id")
    ReviewResponse.ReviewDetailResponse toReviewResponse(Review review);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "raisedById", source = "raisedBy.id")
    @Mapping(target = "assignedAdminId", source = "assignedAdmin.id")
    DisputeResponse.DisputeDetailResponse toDisputeResponse(Dispute dispute);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", source = "sender.fullName")
    MessageResponse.MessageDetailResponse toMessageResponse(Message message);
}
