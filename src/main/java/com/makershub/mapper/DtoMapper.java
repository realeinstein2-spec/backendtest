package com.makershub.mapper;

import com.makershub.dto.response.*;
import com.makershub.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DtoMapper {

    @Mapping(target = "role", source = "role")
    @Mapping(target = "otpCode", ignore = true)
    @Mapping(target = "payoutAccountType", source = "factory.payoutAccountType")
    @Mapping(target = "payoutAccountName", source = "factory.payoutAccountName")
    @Mapping(target = "payoutAccountNumber", source = "factory.payoutAccountNumber")
    @Mapping(target = "payoutBankCode", source = "factory.payoutBankCode")
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
    @Mapping(target = "factoryLatitude", expression = "java(bid.getFactory().getLatitude())")
    @Mapping(target = "factoryLongitude", expression = "java(bid.getFactory().getLongitude())")
    @Mapping(target = "factoryAddress", source = "factory.address")
    @Mapping(target = "factoryDescription", source = "factory.description")
    @Mapping(target = "factoryMinOrderQuantity", source = "factory.minOrderQuantity")
    @Mapping(target = "factoryMaxOrderQuantity", source = "factory.maxOrderQuantity")
    @Mapping(target = "factoryCompletionRate", source = "factory.completionRate")
    @Mapping(target = "factoryResponseTimeHours", source = "factory.responseTimeHours")
    @Mapping(target = "factoryTotalOrders", source = "factory.user.totalOrders")
    @Mapping(target = "factoryCoverImageUrl", source = "factory.user.coverImageUrl")
    @Mapping(target = "factoryRegion", source = "factory.user.region")
    @Mapping(target = "factoryTown", source = "factory.user.town")
    @Mapping(target = "factoryVerificationStatus", source = "factory.verificationStatus")
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

    @Mapping(target = "ownerId", source = "user.id")
    @Mapping(target = "ownerName", source = "user.fullName")
    @Mapping(target = "ownerPhoneNumber", source = "user.phoneNumber")
    @Mapping(target = "latitude", expression = "java(factory.getLatitude())")
    @Mapping(target = "longitude", expression = "java(factory.getLongitude())")
    FactoryResponse.FactoryDetailResponse toFactoryResponse(Factory factory);

    @Mapping(target = "factoryId", source = "id")
    @Mapping(target = "factoryCreatedAt", source = "createdAt")
    @Mapping(target = "ownerId", source = "user.id")
    @Mapping(target = "ownerName", source = "user.fullName")
    @Mapping(target = "ownerPhoneNumber", source = "user.phoneNumber")
    @Mapping(target = "ownerEmail", source = "user.email")
    @Mapping(target = "ownerRegion", source = "user.region")
    @Mapping(target = "ownerTown", source = "user.town")
    @Mapping(target = "profileImageUrl", source = "user.profileImageUrl")
    @Mapping(target = "coverImageUrl", source = "user.coverImageUrl")
    @Mapping(target = "ratingAvg", source = "user.ratingAvg")
    @Mapping(target = "totalOrders", source = "user.totalOrders")
    @Mapping(target = "memberSince", source = "user.createdAt")
    @Mapping(target = "latitude", expression = "java(factory.getLatitude())")
    @Mapping(target = "longitude", expression = "java(factory.getLongitude())")
    FactoryResponse.FactoryPublicProfileResponse toFactoryPublicProfile(Factory factory);
}
