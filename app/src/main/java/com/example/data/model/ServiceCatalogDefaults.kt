package com.example.data.model

object ServiceCatalogDefaults {

    data class SeedService(
        val id: String,
        val name: String,
        val category: ServiceCategory,
        val price: Double = 0.0,
        val fixedPrice: Boolean = false,
        val billingType: BillingType = BillingType.CUSTOM_QUOTE,
        val description: String = ""
    )

    val defaultServices: List<SeedService> = listOf(
        // Digital Marketing
        SeedService("dm_social_media_mgmt", "Social Media Management", ServiceCategory.DIGITAL_MARKETING),
        SeedService(
            "dm_instagram_mgmt",
            "Instagram Management",
            ServiceCategory.DIGITAL_MARKETING,
            price = 14999.0,
            fixedPrice = true,
            billingType = BillingType.MONTHLY,
            description = "Complete Instagram account management, content scheduling, hashtag optimization, and monthly growth insights."
        ),
        SeedService("dm_facebook_mgmt", "Facebook Management", ServiceCategory.DIGITAL_MARKETING),
        SeedService("dm_insta_fb_mkt", "Instagram & Facebook Marketing", ServiceCategory.DIGITAL_MARKETING),
        SeedService("dm_insta_opt", "Instagram Page Optimization", ServiceCategory.DIGITAL_MARKETING),
        SeedService("dm_meta_ads", "Meta Ads Management", ServiceCategory.DIGITAL_MARKETING),
        SeedService("dm_google_ads", "Google Ads Management", ServiceCategory.DIGITAL_MARKETING),
        SeedService("dm_youtube_ads", "YouTube Ads Management", ServiceCategory.DIGITAL_MARKETING),
        SeedService(
            "dm_youtube_seo",
            "YouTube SEO & Growth",
            ServiceCategory.DIGITAL_MARKETING,
            price = 9999.0,
            fixedPrice = true,
            billingType = BillingType.MONTHLY,
            description = "Keyword research, video SEO optimization, CTR thumbnail strategy, and channel growth analytics."
        ),
        SeedService("dm_strategy", "Digital Marketing Strategy", ServiceCategory.DIGITAL_MARKETING),
        SeedService("dm_mkt_strategy", "Marketing Strategy", ServiceCategory.DIGITAL_MARKETING),
        SeedService("dm_lead_gen", "Lead Generation", ServiceCategory.DIGITAL_MARKETING),
        SeedService("dm_brand_growth", "Brand Growth Consulting", ServiceCategory.DIGITAL_MARKETING),

        // Content Creation
        SeedService("cc_content_creation", "Content Creation", ServiceCategory.CONTENT_CREATION),
        SeedService("cc_graphic_design", "Graphic Designing", ServiceCategory.CONTENT_CREATION),
        SeedService("cc_social_post_design", "Social Media Post Designing", ServiceCategory.CONTENT_CREATION),
        SeedService(
            "cc_reels_shorts_pack",
            "Reels/Shorts Content",
            ServiceCategory.CONTENT_CREATION,
            price = 7999.0,
            fixedPrice = true,
            billingType = BillingType.MONTHLY,
            description = "High-retention monthly Reels and YouTube Shorts scripting, editing, captions, and pacing."
        ),
        SeedService("cc_reels_creation", "Reels Creation", ServiceCategory.CONTENT_CREATION),
        SeedService("cc_shorts_creation", "Shorts Creation", ServiceCategory.CONTENT_CREATION),
        SeedService("cc_video_editing", "Video Editing", ServiceCategory.CONTENT_CREATION),
        SeedService("cc_promo_video", "Promotional Video Creation", ServiceCategory.CONTENT_CREATION),
        SeedService("cc_motion_graphics", "Motion Graphics", ServiceCategory.CONTENT_CREATION),
        SeedService("cc_2d_animation", "2D Animation", ServiceCategory.CONTENT_CREATION),
        SeedService("cc_3d_animation", "3D Animation", ServiceCategory.CONTENT_CREATION),
        SeedService("cc_logo_design", "Logo Design", ServiceCategory.CONTENT_CREATION),
        SeedService("cc_brand_identity", "Brand Identity Design", ServiceCategory.CONTENT_CREATION),

        // AI & Automation
        SeedService(
            "ai_automation_setup",
            "Automation Setup",
            ServiceCategory.AI_AUTOMATION,
            price = 17999.0,
            fixedPrice = true,
            billingType = BillingType.ONE_TIME,
            description = "End-to-end business workflow, WhatsApp funnel, and CRM automation setup."
        ),
        SeedService("ai_voice_agents", "AI Voice Agents", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_whatsapp_agents", "WhatsApp AI Agents", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_whatsapp_automation", "WhatsApp Automation", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_chatbots", "AI Chatbots", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_website_chatbots", "Website Chatbots", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_workflow_auto", "Workflow Automation", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_n8n_auto", "n8n Automation", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_zapier_auto", "Zapier Automation", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_lead_followup", "Lead Follow-up Automation", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_customer_support", "Customer Support Automation", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_business_process", "Business Process Automation", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_crm_auto", "CRM Automation", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_integration", "AI Integration", ServiceCategory.AI_AUTOMATION),
        SeedService("ai_custom_solutions", "Custom AI Solutions", ServiceCategory.AI_AUTOMATION),

        // Website & E-commerce
        SeedService("web_business", "Business Website Development", ServiceCategory.WEBSITE_ECOMMERCE),
        SeedService("web_landing_page", "Landing Page Development", ServiceCategory.WEBSITE_ECOMMERCE),
        SeedService("web_portfolio", "Portfolio Website Development", ServiceCategory.WEBSITE_ECOMMERCE),
        SeedService("web_ecommerce", "E-commerce Website Development", ServiceCategory.WEBSITE_ECOMMERCE),
        SeedService(
            "web_shopify_setup",
            "Shopify Store Setup",
            ServiceCategory.WEBSITE_ECOMMERCE,
            price = 24999.0,
            fixedPrice = true,
            billingType = BillingType.ONE_TIME,
            description = "Complete Shopify store setup, theme configuration, product catalogue, and payment gateway readiness."
        ),
        SeedService("web_shopify_custom", "Shopify Store Customization", ServiceCategory.WEBSITE_ECOMMERCE),
        SeedService("web_redesign", "Website Redesign", ServiceCategory.WEBSITE_ECOMMERCE),
        SeedService("web_maintenance", "Website Maintenance", ServiceCategory.WEBSITE_ECOMMERCE),
        SeedService("web_payment_gateway", "Payment Gateway Integration", ServiceCategory.WEBSITE_ECOMMERCE),
        SeedService("web_seo", "Website SEO", ServiceCategory.WEBSITE_ECOMMERCE),

        // Software & Business Solutions
        SeedService("sw_android_app", "Custom Android App Development", ServiceCategory.SOFTWARE_SOLUTIONS),
        SeedService("sw_business_mgmt", "Business Management Software", ServiceCategory.SOFTWARE_SOLUTIONS),
        SeedService("sw_school_mgmt", "School Management Software", ServiceCategory.SOFTWARE_SOLUTIONS),
        SeedService("sw_crm_dev", "CRM Development", ServiceCategory.SOFTWARE_SOLUTIONS),
        SeedService("sw_admin_dash", "Admin Dashboard Development", ServiceCategory.SOFTWARE_SOLUTIONS),
        SeedService("sw_business_auto", "Business Automation Systems", ServiceCategory.SOFTWARE_SOLUTIONS),
        SeedService("sw_custom_solutions", "Custom Software Solutions", ServiceCategory.SOFTWARE_SOLUTIONS),

        // Consulting
        SeedService(
            "con_one_on_one",
            "1-on-1 Consulting",
            ServiceCategory.CONSULTING,
            price = 4999.0,
            fixedPrice = true,
            billingType = BillingType.PER_SESSION,
            description = "Focused 1-on-1 growth, WhatsApp marketing, and digital strategy consulting session."
        ),
        SeedService("con_digital_marketing", "Digital Marketing Consulting", ServiceCategory.CONSULTING),
        SeedService("con_business_growth", "Business Growth Consulting", ServiceCategory.CONSULTING),
        SeedService("con_automation", "Automation Consulting", ServiceCategory.CONSULTING),
        SeedService("con_social_strategy", "Social Media Strategy Consulting", ServiceCategory.CONSULTING)
    )
}
