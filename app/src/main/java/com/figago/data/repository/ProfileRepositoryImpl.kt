package com.figago.data.repository

import com.figago.data.dao.ProfileDao
import com.figago.data.entity.ProfileEntity
import com.figago.domain.repository.ProfileRepository
import com.figago.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val settingsRepository: SettingsRepository
) : ProfileRepository {

    override fun getAllProfiles(): Flow<List<ProfileEntity>> {
        return profileDao.getAllProfilesFlow()
    }

    override suspend fun getProfileById(id: Long): ProfileEntity? {
        return profileDao.getProfileById(id)
    }

    override fun getProfileByIdFlow(id: Long): Flow<ProfileEntity?> {
        return profileDao.getProfileByIdFlow(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getActiveProfile(): Flow<ProfileEntity?> {
        return settingsRepository.observeActiveProfileId().flatMapLatest { profileId ->
            profileDao.getProfileByIdFlow(profileId)
        }
    }

    override suspend fun saveProfile(profile: ProfileEntity): Long {
        return profileDao.insertProfile(profile)
    }

    override suspend fun deleteProfile(profile: ProfileEntity) {
        profileDao.deleteProfile(profile)
    }
}
