(() => {
  const TAB_KEY = "tab";

  const createTabState = (allowedTabs) => {
    return {
      activeTab: "queue",
      showFilters: false,
      searchQuery: "",
      searchMergeQuery: "",
      init() {
        const fromUrl = new URLSearchParams(window.location.search).get(
          TAB_KEY,
        );
        this.activeTab = allowedTabs.includes(fromUrl)
          ? fromUrl
          : allowedTabs[0];
        this.syncTab();
      },
      setTab(tab) {
        if (!allowedTabs.includes(tab)) {
          return;
        }

        this.activeTab = tab;
        this.syncTab();
      },
      syncTab() {
        const currentUrl = new URL(window.location.href);
        currentUrl.searchParams.set(TAB_KEY, this.activeTab);
        window.history.replaceState({}, "", currentUrl);
      },
    };
  };

  window.adminDashboardState = (role) => {
    const isDinasRole = role === "admin_dinas";
    return createTabState(
      isDinasRole
        ? ["penugasan", "progress", "close"]
        : ["queue", "merge", "disposisi", "ditolak"],
    );
  };

  window.adminDinasDashboardState = () =>
    createTabState(["penugasan", "sengketa"]);

  window.adminLoginForm = () => ({
    showPassword: false,
    loading: false,
    showOtpModal: false,
    otp: "",
    validationError: "",

    openOtpModal() {
      const form = this.$refs.loginForm;
      const role = form?.querySelector("#role")?.value?.trim();
      const email = form?.querySelector("#email")?.value?.trim();
      const password = form?.querySelector("#password")?.value;

      if (!role || !email || !password) {
        this.validationError =
          "Role, email/nomor HP, dan password wajib diisi.";
        return;
      }

      if (password.length < 6) {
        this.validationError = "Password minimal 6 karakter.";
        return;
      }

      this.validationError = "";
      this.showOtpModal = true;
      this.otp = "";
    },

    closeOtpModal() {
      if (this.loading) {
        return;
      }
      this.showOtpModal = false;
      this.validationError = "";
    },

    submitWithOtp() {
      const normalizedOtp = this.otp.replace(/\s+/g, "");

      if (!/^\d{6}$/.test(normalizedOtp)) {
        this.validationError = "Kode OTP harus 6 digit angka.";
        return;
      }

      this.loading = true;
      this.validationError = "";
      this.otp = normalizedOtp;
      this.$nextTick(() => {
        this.$refs.loginForm.submit();
      });
    },
  });

  // Auto-fill datetime-local inputs with sensible defaults
  document.addEventListener("DOMContentLoaded", () => {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    const currentIso = now.toISOString().slice(0, 16);

    document
      .querySelectorAll('input[type="datetime-local"]')
      .forEach((input) => {
        if (!input.value) {
          input.value = currentIso;
        }
        if (!input.min) {
          input.min = currentIso;
        }
      });

    initLightbox();
  });

  function initLightbox() {
    var overlay = document.createElement("div");
    overlay.className = "aduaja-lightbox";
    overlay.innerHTML =
      '<button class="close-btn">&times;</button><img src="" alt="Preview" />';
    document.body.appendChild(overlay);

    var img = overlay.querySelector("img");
    var closeBtn = overlay.querySelector(".close-btn");

    function open(src) {
      img.src = src;
      overlay.classList.add("open");
      document.body.style.overflow = "hidden";
    }

    function close() {
      overlay.classList.remove("open");
      document.body.style.overflow = "";
      setTimeout(function () {
        img.src = "";
      }, 300);
    }

    overlay.addEventListener("click", function (e) {
      if (e.target === overlay || e.target === closeBtn) {
        close();
      }
    });

    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && overlay.classList.contains("open")) {
        close();
      }
    });

    document.addEventListener("click", function (e) {
      var target = e.target.closest("[data-lightbox]");
      if (target) {
        e.preventDefault();
        var src = target.getAttribute("src") || target.getAttribute("data-lightbox");
        if (src) open(src);
      }
    });
  }
})();
